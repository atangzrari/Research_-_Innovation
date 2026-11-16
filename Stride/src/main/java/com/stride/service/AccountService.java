package com.stride.service;

import com.stride.dao.AuditDao;
import com.stride.dao.UserDao;
import com.stride.model.Role;
import com.stride.model.User;
import com.stride.security.PasswordHasher;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Sign-in and account management (RQ01, RQ02, RQ20). Accounts live in the database (RQ26). */
public class AccountService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> COMMON = Set.of("password", "password1", "password123", "1234567890",
            "12345678", "123456789", "qwerty123", "letmein123", "iloveyou", "welcome123", "admin12345",
            "botswana123", "gaborone123", "changeme", "changeme123", "stride123", "qwertyuiop");
    private static final String TEMP_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserDao users;
    private final AuditDao audit;
    private final Clock clock;
    private final ZoneId zone;
    // Hashed once so that an unknown e-mail costs the same time as a wrong password.
    private final String dummyHash;

    public AccountService(UserDao users, AuditDao audit, Clock clock, ZoneId zone) {
        this.users = users;
        this.audit = audit;
        this.clock = clock;
        this.zone = zone;
        this.dummyHash = PasswordHasher.hash("not-a-real-password");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(zone));
    }

    public static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Checks an e-mail and password. Unknown e-mail, wrong password and a deactivated account all
     * give the same empty answer in about the same time, so accounts cannot be probed.
     */
    public Optional<User> authenticate(String email, String password, String ip) {
        Optional<UserDao.Account> account = users.findAccountByEmail(normalise(email));
        if (account.isEmpty() || account.get().passwordHash == null) {
            PasswordHasher.verify(password == null ? "" : password, dummyHash);
            return Optional.empty();
        }
        UserDao.Account a = account.get();
        if (!PasswordHasher.verify(password, a.passwordHash) || !a.user.isActive()) {
            if (a.user.isActive()) {
                audit.log(now(), a.user.getUserId(), "SIGN_IN_FAILED", "Wrong password", ip);
            }
            return Optional.empty();
        }
        if (PasswordHasher.needsRehash(a.passwordHash)) {
            users.setPassword(a.user.getUserId(), PasswordHasher.hash(password), a.user.isMustChangePassword());
        }
        users.touchLogin(a.user.getUserId(), now());
        audit.log(now(), a.user.getUserId(), "SIGN_IN", "Password sign-in", ip);
        return users.find(a.user.getUserId());
    }

    /** Signs in a person already verified by the college identity provider (RQ27). */
    public Optional<User> signInVerified(String email, String ip) {
        Optional<User> u = users.findByEmail(normalise(email)).filter(User::isActive);
        u.ifPresent(user -> {
            users.touchLogin(user.getUserId(), now());
            audit.log(now(), user.getUserId(), "SIGN_IN", "Single sign-on", ip);
        });
        return u;
    }

    public Optional<User> find(int id) {
        return users.find(id);
    }

    /** Problems with a proposed password, or null if it is acceptable (NIST SP 800-63B style rules). */
    public static String passwordProblem(String password, String email) {
        if (password == null || password.length() < 10) {
            return "Use at least 10 characters. A short sentence is easy to remember.";
        }
        if (password.length() > 128) {
            return "Use 128 characters or fewer.";
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON.contains(lower)) {
            return "That password is too common. Choose something less guessable.";
        }
        String local = normalise(email);
        int at = local.indexOf('@');
        if (at > 2 && lower.contains(local.substring(0, at))) {
            return "Don't use your e-mail name in your password.";
        }
        return null;
    }

    public void changePassword(User user, String current, String next, String repeat) {
        UserDao.Account a = users.findAccount(user.getUserId())
                .orElseThrow(() -> new ServiceException("Your account no longer exists."));
        if (a.passwordHash == null || !PasswordHasher.verify(current, a.passwordHash)) {
            throw new ServiceException("Your current password is not right.");
        }
        if (next == null || !next.equals(repeat)) {
            throw new ServiceException("The two new passwords are different. Type the same one twice.");
        }
        if (next.equals(current)) {
            throw new ServiceException("Choose a password different from the current one.");
        }
        String problem = passwordProblem(next, user.getEmail());
        if (problem != null) {
            throw new ServiceException(problem);
        }
        users.setPassword(user.getUserId(), PasswordHasher.hash(next), false);
        audit.log(now(), user.getUserId(), "PASSWORD_CHANGE", "Changed own password", null);
    }

    // ------------------------------------------------------------------ administration (RQ20)

    public List<User> list(Role role, String search) {
        return users.list(role, search);
    }

    /** Creates an account and returns its one-time temporary password. */
    public String create(User admin, String first, String last, String email, Role role, String studentNumber) {
        requireAdmin(admin);
        String f = required(first, "first name", 80);
        String l = required(last, "last name", 80);
        String e = normalise(email);
        if (!EMAIL.matcher(e).matches() || e.length() > 190) {
            throw new ServiceException("Enter a valid e-mail address.");
        }
        if (role == null) {
            throw new ServiceException("Choose a role.");
        }
        if (users.findByEmail(e).isPresent()) {
            throw new ServiceException("An account with that e-mail already exists.");
        }
        String number = blankToNull(studentNumber);
        if (role == Role.STUDENT && number == null) {
            throw new ServiceException("Students need a student number.");
        }
        if (number != null && users.findByStudentNumber(number).isPresent()) {
            throw new ServiceException("That student number is already in use.");
        }
        String temp = temporaryPassword();
        int id = users.create(f, l, e, role, role == Role.STUDENT ? number : null, PasswordHasher.hash(temp), true, now());
        audit.log(now(), admin.getUserId(), "USER_CREATE", role.getLabel() + " " + e + " (id " + id + ")", null);
        return temp;
    }

    public void update(User admin, int id, String first, String last, String email, Role role, String studentNumber) {
        requireAdmin(admin);
        User existing = users.find(id).orElseThrow(() -> new ServiceException("That account does not exist."));
        String e = normalise(email);
        if (!EMAIL.matcher(e).matches()) {
            throw new ServiceException("Enter a valid e-mail address.");
        }
        Optional<User> clash = users.findByEmail(e);
        if (clash.isPresent() && clash.get().getUserId() != id) {
            throw new ServiceException("Another account already uses that e-mail.");
        }
        if (id == admin.getUserId() && role != Role.ADMINISTRATOR) {
            throw new ServiceException("You cannot remove your own administrator role.");
        }
        String number = role == Role.STUDENT ? blankToNull(studentNumber) : null;
        if (role == Role.STUDENT && number == null) {
            throw new ServiceException("Students need a student number.");
        }
        users.update(id, required(first, "first name", 80), required(last, "last name", 80), e, role, number);
        audit.log(now(), admin.getUserId(), "USER_UPDATE", existing.getEmail() + " -> " + e + ", " + role, null);
    }

    public void setActive(User admin, int id, boolean active) {
        requireAdmin(admin);
        if (id == admin.getUserId() && !active) {
            throw new ServiceException("You cannot deactivate your own account.");
        }
        User u = users.find(id).orElseThrow(() -> new ServiceException("That account does not exist."));
        users.setActive(id, active);
        audit.log(now(), admin.getUserId(), active ? "USER_ACTIVATE" : "USER_DEACTIVATE", u.getEmail(), null);
    }

    /** Sets a new temporary password that must be changed at next sign-in, and returns it once. */
    public String resetPassword(User admin, int id) {
        requireAdmin(admin);
        User u = users.find(id).orElseThrow(() -> new ServiceException("That account does not exist."));
        String temp = temporaryPassword();
        users.setPassword(id, PasswordHasher.hash(temp), true);
        audit.log(now(), admin.getUserId(), "PASSWORD_RESET", u.getEmail(), null);
        return temp;
    }

    public java.util.Map<Role, Integer> countByRole() {
        return users.countByRole();
    }

    static void requireAdmin(User user) {
        if (user == null || !user.isAdministrator()) {
            throw new ServiceException("Only an administrator can do that.");
        }
    }

    static String required(String value, String label, int max) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            throw new ServiceException("Enter the " + label + ".");
        }
        if (v.length() > max) {
            throw new ServiceException("The " + label + " is too long (" + max + " characters at most).");
        }
        return v;
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static String temporaryPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i == 4 || i == 8) {
                sb.append('-');
            }
            sb.append(TEMP_ALPHABET.charAt(RANDOM.nextInt(TEMP_ALPHABET.length())));
        }
        return sb.toString();
    }
}
