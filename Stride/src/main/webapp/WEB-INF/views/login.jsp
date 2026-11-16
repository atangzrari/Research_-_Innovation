<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Sign in to Stride, one place for Gradebook, Moodle, the digital library, PrimeTimetable and ThutoAcademics.">
    <meta name="theme-color" content="#ABCAE9">
    <title>Sign in | Stride</title>
    <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Raleway:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="${ctx}/assets/css/stride.css">
    <script src="${ctx}/assets/js/size.js"></script>
    <script src="${ctx}/assets/js/login.js" defer></script>
</head>
<body>
<a class="skip" href="#signin">Skip to sign in</a>

<div class="auth">
    <aside class="auth-intro" aria-labelledby="intro-title">
        <a class="brand" href="${ctx}/login" aria-label="Stride home">
            <svg viewBox="0 0 32 32" aria-hidden="true" focusable="false">
                <rect x="2" y="20" width="8" height="10" fill="#000"/>
                <rect x="12" y="12" width="8" height="18" fill="#000"/>
                <rect x="22" y="4" width="8" height="26" fill="#000"/>
            </svg>
            <span>Stride</span>
        </a>

        <div>
            <h2 class="intro-title" id="intro-title">Every college site, one sign-in away.</h2>
            <ul class="intro-list">
                <c:forEach var="s" items="${services}">
                    <li><strong><c:out value="${s.name}"/></strong>
                        <span><c:out value="${s.description}"/></span></li>
                </c:forEach>
            </ul>
            <p class="intro-more">Plus your classes, rooms free right now, attendance check-in, deadlines and notices.</p>
        </div>
    </aside>

    <main class="auth-main" id="signin">
        <div class="auth-card">
            <h1>Sign in</h1>
            <p class="lede">Use your Stride account.</p>

            <c:if test="${not empty message}">
                <p class="notice notice-error" role="alert"><strong>Couldn’t sign in.</strong>&#32;<c:out value="${message}"/></p>
            </c:if>
            <c:if test="${empty message and param.signedout == '1'}">
                <p class="notice" role="status"><strong>You’re signed out.</strong>&#32;Sign in again whenever you’re ready.</p>
            </c:if>
            <c:if test="${param.sso == 'noaccount'}">
                <p class="notice notice-error" role="alert"><strong>No Stride account.</strong>&#32;Your college account signed in, but no active Stride account uses that e-mail. Ask ICT Support.</p>
            </c:if>
            <c:if test="${param.sso == 'failed' or param.sso == 'cancelled'}">
                <p class="notice notice-error" role="alert"><strong>College sign-in didn’t finish.</strong>&#32;Try again, or use your Stride password.</p>
            </c:if>

            <c:if test="${ssoEnabled}">
                <a class="button button-sso" href="${ctx}/sso/start"><c:out value="${ssoLabel}"/></a>
                <p class="or"><span>or</span></p>
            </c:if>

            <form action="${ctx}/login" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

                <div class="field">
                    <label for="email">Email address</label>
                    <input class="control" type="email" id="email" name="email" required
                           autocomplete="username" autocapitalize="none" spellcheck="false"
                           placeholder="you@college.ac.bw"
                           value="<c:out value='${param.email}'/>">
                </div>

                <div class="field">
                    <label for="password">Password</label>
                    <div class="password-wrap">
                        <input class="control" type="password" id="password" name="password" required
                               autocomplete="current-password">
                        <button class="toggle" type="button" id="toggle-password" hidden
                                aria-pressed="false" aria-label="Show password">Show</button>
                    </div>
                </div>

                <button class="button" type="submit">Sign in</button>
            </form>

            <p class="help">Trouble signing in? Contact ICT Support.</p>
        </div>
    </main>
</div>
</body>
</html>
