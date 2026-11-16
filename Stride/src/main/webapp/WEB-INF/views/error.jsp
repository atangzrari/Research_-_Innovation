<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="code" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Something went wrong | Stride</title>
    <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Raleway:wght@400;600;700;800&display=swap">
    <link rel="stylesheet" href="${ctx}/assets/css/stride.css">
</head>
<body>
<main class="error-page">
    <a class="brand" href="${ctx}/dashboard" aria-label="Stride home">
        <svg viewBox="0 0 32 32" aria-hidden="true" focusable="false">
            <rect x="2" y="20" width="8" height="10" fill="#000"/>
            <rect x="12" y="12" width="8" height="18" fill="#000"/>
            <rect x="22" y="4" width="8" height="26" fill="#000"/>
        </svg>
        <span>Stride</span>
    </a>
    <c:choose>
        <c:when test="${code == 404}">
            <h1>That page doesn’t exist.</h1>
            <p>The address may be mistyped, or the page may have moved.</p>
        </c:when>
        <c:when test="${code == 403}">
            <h1>That action wasn’t allowed.</h1>
            <p>Your account may not have access to that page, or your session expired. Go back home and try again.</p>
        </c:when>
        <c:when test="${code == 400 or code == 405}">
            <h1>Stride couldn’t understand that request.</h1>
            <p>Go back and try again from the page itself.</p>
        </c:when>
        <c:otherwise>
            <h1>Something went wrong on our side.</h1>
            <p>Nothing you did caused this. Try again in a moment.</p>
        </c:otherwise>
    </c:choose>
    <a class="button" href="${ctx}/dashboard">Back to home</a>
</main>
</body>
</html>
