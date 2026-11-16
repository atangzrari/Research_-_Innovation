<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${s.moduleCode}"/> check-in | Stride</title>
    <noscript><meta http-equiv="refresh" content="${secondsLeft + 1}"></noscript>
    <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Raleway:wght@400;600;700;800&display=swap">
    <link rel="stylesheet" href="${ctx}/assets/css/stride.css">
</head>
<body class="projector">
<main class="live projector-main" data-live="${ctx}/lecturer/live?id=${s.id}" data-qr="${ctx}/lecturer/qr?id=${s.id}">
    <header>
        <p class="projector-module"><c:out value="${s.moduleCode} ${s.moduleName}"/></p>
        <h1>Check in now</h1>
    </header>
    <c:choose>
        <c:when test="${open}">
            <div class="projector-body">
                <img class="qr qr-big" data-qr-img src="${ctx}/lecturer/qr?id=${s.id}" alt="QR code to check in">
                <div>
                    <p class="projector-step">Scan the QR code with your phone camera, or open Stride, choose <strong>Check in</strong> and type:</p>
                    <p class="code code-big" data-code><c:out value="${code}"/></p>
                    <p class="projector-small">New code in <span data-seconds><c:out value="${secondsLeft}"/></span> s · <span data-count><c:out value="${s.checkedIn}"/></span> checked in · closes <span data-closes><c:out value="${s.closesText}"/></span></p>
                    <p class="projector-small"><c:out value="${checkinUrl}"/></p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <p class="code code-big">Closed</p>
        </c:otherwise>
    </c:choose>
</main>
<script src="${ctx}/assets/js/live.js" defer></script>
</body>
</html>
