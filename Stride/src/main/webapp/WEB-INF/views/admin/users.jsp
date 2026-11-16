<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="People" active="users">
    <header class="page-head">
        <h1>People</h1>
        <p class="lead">Student, lecturer and administrator accounts.</p>
    </header>

    <form class="prefs" action="${ctx}/admin/users" method="get" role="search" aria-label="Filter people">
        <div class="field"><label for="f-role">Role</label>
            <select class="control" id="f-role" name="role">
                <option value="">Everyone</option>
                <c:forEach var="r" items="${roles}"><option value="${r.name}" ${roleFilter == r.name ? 'selected' : ''}><c:out value="${r.label}"/></option></c:forEach>
            </select></div>
        <div class="field"><label for="f-q">Name, e-mail or number</label><input class="control" id="f-q" name="q" value="<c:out value='${search}'/>"></div>
        <button class="button" type="submit">Filter</button>
    </form>

    <div class="table-wrap">
        <table class="table">
            <caption class="visually-hidden">Accounts</caption>
            <thead><tr><th scope="col">Name</th><th scope="col">E-mail</th><th scope="col">Role</th><th scope="col">Student number</th><th scope="col">State</th><th scope="col">Last sign-in</th></tr></thead>
            <tbody>
            <c:forEach var="u" items="${users}">
                <tr>
                    <th scope="row"><a href="${ctx}/admin/user?id=${u.id}"><c:out value="${u.fullName}"/></a></th>
                    <td><c:out value="${u.email}"/></td>
                    <td><c:out value="${u.role.label}"/></td>
                    <td><c:out value="${u.studentNumber}"/></td>
                    <td><c:out value="${u.active ? 'Active' : 'Switched off'}"/></td>
                    <td><c:out value="${empty u.lastLoginAt ? 'Never' : u.lastLoginAt.toLocalDate()}"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    <p class="muted small"><c:out value="${users.size()} accounts"/></p>

    <form class="form-card" id="new" action="${ctx}/admin/users/create" method="post" aria-labelledby="new-title">
        <h2 id="new-title">Create an account</h2>
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <div class="form-grid">
            <div class="field"><label for="first">First name</label><input class="control" id="first" name="first" required maxlength="80"></div>
            <div class="field"><label for="last">Last name</label><input class="control" id="last" name="last" required maxlength="80"></div>
            <div class="field"><label for="email">E-mail</label><input class="control" type="email" id="email" name="email" required maxlength="190"></div>
            <div class="field"><label for="role">Role</label>
                <select class="control" id="role" name="role"><c:forEach var="r" items="${roles}"><option value="${r.name}"><c:out value="${r.label}"/></option></c:forEach></select></div>
            <div class="field"><label for="number">Student number (students only)</label><input class="control" id="number" name="number" maxlength="30"></div>
        </div>
        <p class="hint">Stride makes a temporary password and shows it once. The person must choose their own when they first sign in.</p>
        <button class="button" type="submit">Create account</button>
    </form>
</s:shell>
