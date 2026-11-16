<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="active" required="true" %>
<%@ attribute name="wide" required="false" type="java.lang.Boolean" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="me" value="${sessionScope.strideUser}"/>
<c:set var="role" value="${me.role.name}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#ABCAE9">
    <title><c:out value="${title}"/> | Stride</title>
    <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Raleway:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="${ctx}/assets/css/stride.css">
    <script src="${ctx}/assets/js/size.js"></script>
    <script src="${ctx}/assets/js/app.js" defer></script>
</head>
<body data-ctx="<c:out value='${ctx}'/>">
<%-- icon sprite: drawn once, reused with <use href="#i-name"> --%>
<svg class="sprite" width="0" height="0" aria-hidden="true" focusable="false">
    <defs>
        <symbol id="i-home" viewBox="0 0 24 24"><path d="M3 11l9-8 9 8M5 10v10h14V10M10 20v-6h4v6"/></symbol>
        <symbol id="i-classes" viewBox="0 0 24 24"><path d="M4 6h16v14H4zM4 10h16M8 3v4M16 3v4"/></symbol>
        <symbol id="i-notices" viewBox="0 0 24 24"><path d="M6 9a6 6 0 0112 0c0 6 2 7 2 7H4s2-1 2-7zM10 20a2 2 0 004 0"/></symbol>
        <symbol id="i-attendance" viewBox="0 0 24 24"><path d="M12 21a9 9 0 100-18 9 9 0 000 18zM8 12l3 3 5-6"/></symbol>
        <symbol id="i-sites" viewBox="0 0 24 24"><path d="M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z"/></symbol>
        <symbol id="i-help" viewBox="0 0 24 24"><path d="M12 21a9 9 0 100-18 9 9 0 000 18zM9.5 9.5a2.5 2.5 0 114 2c-1 .7-1.5 1.2-1.5 2.5M12 17h.01"/></symbol>
        <symbol id="i-search" viewBox="0 0 24 24"><path d="M11 18a7 7 0 100-14 7 7 0 000 14zM21 21l-5-5"/></symbol>
        <symbol id="i-external" viewBox="0 0 24 24"><path d="M14 4h6v6M20 4l-9 9M18 14v6H4V6h6"/></symbol>
        <symbol id="i-gradebook" viewBox="0 0 24 24"><path d="M9 4h6v3H9zM6 5h12v16H6zM9 12h6M9 16h4"/></symbol>
        <symbol id="i-moodle" viewBox="0 0 24 24"><path d="M12 6c-2-2-5-2.5-8-2v14c3-.5 6 0 8 2 2-2 5-2.5 8-2V4c-3-.5-6 0-8 2zM12 6v14"/></symbol>
        <symbol id="i-timetable" viewBox="0 0 24 24"><path d="M12 21a9 9 0 100-18 9 9 0 000 18zM12 7v5l3 2"/></symbol>
        <symbol id="i-library" viewBox="0 0 24 24"><path d="M5 4h11a3 3 0 013 3v13H8a3 3 0 01-3-3zM5 17a3 3 0 013-3h11"/></symbol>
        <symbol id="i-thuto" viewBox="0 0 24 24"><path d="M7 3h8l4 4v14H7zM14 3v5h5M10 14h6M10 17h4"/></symbol>
        <symbol id="i-checkin" viewBox="0 0 24 24"><path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h2v2h-2zM18 14h2M14 18h2v2M18 18h2v2h-2"/></symbol>
        <symbol id="i-planner" viewBox="0 0 24 24"><path d="M4 6h16v14H4zM4 10h16M8 3v4M16 3v4M8 15l2 2 4-4"/></symbol>
        <symbol id="i-assistant" viewBox="0 0 24 24"><path d="M4 5h16v11H9l-5 4zM8 9h8M8 12h5"/></symbol>
        <symbol id="i-modules" viewBox="0 0 24 24"><path d="M4 5l8-2 8 2v14l-8 2-8-2zM12 3v18"/></symbol>
        <symbol id="i-map" viewBox="0 0 24 24"><path d="M12 21s-7-6-7-11a7 7 0 0114 0c0 5-7 11-7 11zM12 12.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z"/></symbol>
        <symbol id="i-reports" viewBox="0 0 24 24"><path d="M4 20h16M7 16v-5M12 16V6M17 16v-8"/></symbol>
        <symbol id="i-people" viewBox="0 0 24 24"><path d="M9 11a4 4 0 100-8 4 4 0 000 8zM2 21v-1a6 6 0 0112 0v1M16 3.5a4 4 0 010 7.5M22 21v-1a6 6 0 00-4-5.6"/></symbol>
        <symbol id="i-rooms" viewBox="0 0 24 24"><path d="M5 21V4h11v17M3 21h18M13 12h.01M16 4l3 1v16"/></symbol>
        <symbol id="i-audit" viewBox="0 0 24 24"><path d="M9 4h10v16H5V8zM9 4v4H5M9 12h6M9 16h6"/></symbol>
        <symbol id="i-session" viewBox="0 0 24 24"><path d="M3 5h18v11H3zM8 20h8M12 16v4M8 10l2 2 4-4"/></symbol>
        <symbol id="i-account" viewBox="0 0 24 24"><path d="M12 12a4 4 0 100-8 4 4 0 000 8zM4 21v-1a8 8 0 0116 0v1"/></symbol>
        <symbol id="i-download" viewBox="0 0 24 24"><path d="M12 4v11M7 10l5 5 5-5M5 20h14"/></symbol>
        <symbol id="i-flag" viewBox="0 0 24 24"><path d="M5 21V4M5 4h11l-2 4 2 4H5"/></symbol>
    </defs>
</svg>

<a class="skip" href="#main">Skip to main content</a>

<div class="app">
    <nav class="side" aria-label="Main">
        <a class="brand" href="${ctx}/dashboard" aria-label="Stride home">
            <svg viewBox="0 0 32 32" aria-hidden="true" focusable="false">
                <rect x="2" y="20" width="8" height="10" fill="#000"/>
                <rect x="12" y="12" width="8" height="18" fill="#000"/>
                <rect x="22" y="4" width="8" height="26" fill="#000"/>
            </svg>
            <span>Stride</span>
        </a>
        <p class="role-chip"><c:out value="${me.role.label}"/></p>
        <ul class="nav">
            <li><a href="${ctx}/dashboard" ${active == 'home' ? 'aria-current="page"' : ''}>
                <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-home"/></svg><span>Home</span></a></li>
            <c:choose>
                <c:when test="${role == 'STUDENT'}">
                    <li><a href="${ctx}/classes" ${active == 'classes' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-classes"/></svg><span>My classes</span></a></li>
                    <li><a href="${ctx}/checkin" ${active == 'checkin' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-checkin"/></svg><span>Check in</span></a></li>
                    <li><a href="${ctx}/attendance" ${active == 'attendance' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-attendance"/></svg><span>Attendance</span></a></li>
                    <li><a href="${ctx}/notices" ${active == 'notices' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-notices"/></svg><span>Notices</span>
                        <c:if test="${unreadCount > 0}"><span class="count"><c:out value="${unreadCount}"/><span class="visually-hidden"> unread</span></span></c:if></a></li>
                    <li><a href="${ctx}/planner" ${active == 'planner' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-planner"/></svg><span>Study planner</span></a></li>
                    <li><a href="${ctx}/assistant" ${active == 'assistant' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-assistant"/></svg><span>Assistant</span></a></li>
                    <li><a href="${ctx}/modules" ${active == 'modules' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-modules"/></svg><span>My modules</span></a></li>
                    <li><a href="${ctx}/map" ${active == 'map' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-map"/></svg><span>Campus map</span></a></li>
                    <li><a href="${ctx}/sites" ${active == 'sites' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-sites"/></svg><span>College sites</span></a></li>
                </c:when>
                <c:when test="${role == 'LECTURER'}">
                    <li><a href="${ctx}/lecturer/sessions" ${active == 'sessions' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-session"/></svg><span>Take attendance</span></a></li>
                    <li><a href="${ctx}/timetable" ${active == 'timetable' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-classes"/></svg><span>My timetable</span></a></li>
                    <li><a href="${ctx}/modules" ${active == 'modules' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-modules"/></svg><span>My modules</span></a></li>
                    <li><a href="${ctx}/notices" ${active == 'notices' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-notices"/></svg><span>Notices</span>
                        <c:if test="${unreadCount > 0}"><span class="count"><c:out value="${unreadCount}"/><span class="visually-hidden"> unread</span></span></c:if></a></li>
                    <li><a href="${ctx}/reports" ${active == 'reports' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-reports"/></svg><span>Reports</span></a></li>
                    <li><a href="${ctx}/classes" ${active == 'classes' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-rooms"/></svg><span>Free rooms</span></a></li>
                    <li><a href="${ctx}/map" ${active == 'map' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-map"/></svg><span>Campus map</span></a></li>
                    <li><a href="${ctx}/sites" ${active == 'sites' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-sites"/></svg><span>College sites</span></a></li>
                </c:when>
                <c:otherwise>
                    <li><a href="${ctx}/admin/users" ${active == 'users' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-people"/></svg><span>People</span></a></li>
                    <li><a href="${ctx}/admin/modules" ${active == 'admin-modules' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-modules"/></svg><span>Modules</span></a></li>
                    <li><a href="${ctx}/admin/timetable" ${active == 'admin-timetable' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-classes"/></svg><span>Timetable</span></a></li>
                    <li><a href="${ctx}/admin/rooms" ${active == 'rooms' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-rooms"/></svg><span>Rooms</span></a></li>
                    <li><a href="${ctx}/notices" ${active == 'notices' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-notices"/></svg><span>Notices</span>
                        <c:if test="${unreadCount > 0}"><span class="count"><c:out value="${unreadCount}"/><span class="visually-hidden"> unread</span></span></c:if></a></li>
                    <li><a href="${ctx}/reports" ${active == 'reports' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-reports"/></svg><span>Reports</span></a></li>
                    <li><a href="${ctx}/admin/sites" ${active == 'admin-sites' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-sites"/></svg><span>College sites</span></a></li>
                    <li><a href="${ctx}/admin/audit" ${active == 'audit' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-audit"/></svg><span>Audit log</span></a></li>
                    <li><a href="${ctx}/map" ${active == 'map' ? 'aria-current="page"' : ''}>
                        <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-map"/></svg><span>Campus map</span></a></li>
                </c:otherwise>
            </c:choose>
        </ul>
    </nav>

    <header class="topbar">
        <a class="brand brand-small" href="${ctx}/dashboard" aria-label="Stride home">
            <svg viewBox="0 0 32 32" aria-hidden="true" focusable="false">
                <rect x="2" y="20" width="8" height="10" fill="#000"/>
                <rect x="12" y="12" width="8" height="18" fill="#000"/>
                <rect x="22" y="4" width="8" height="26" fill="#000"/>
            </svg>
            <span>Stride</span>
        </a>

        <form class="topsearch" action="${ctx}/sites" method="get" role="search">
            <label class="visually-hidden" for="top-search">Search Stride</label>
            <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-search"/></svg>
            <input id="top-search" type="search" name="q" autocomplete="off"
                   placeholder="Search, for example results or CSE305">
            <button class="button" type="submit">Search</button>
        </form>

        <div class="top-actions">
            <div class="textsize" role="group" aria-label="Text size">
                <button type="button" class="ts ts-1" data-size="normal" aria-label="Normal text" aria-pressed="true">A</button>
                <button type="button" class="ts ts-2" data-size="large" aria-label="Large text" aria-pressed="false">A</button>
                <button type="button" class="ts ts-3" data-size="xlarge" aria-label="Extra large text" aria-pressed="false">A</button>
            </div>
            <a class="button button-quiet" href="${ctx}/help" ${active == 'help' ? 'aria-current="page"' : ''}>
                <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-help"/></svg>Help</a>
            <a class="me" href="${ctx}/account" title="${fn:escapeXml(me.fullName)}" ${active == 'account' ? 'aria-current="page"' : ''}>
                <span class="avatar" aria-hidden="true"><c:out value="${me.initials}"/></span>
                <span class="me-name"><c:out value="${me.firstName}"/></span>
                <span class="visually-hidden">Your account</span>
            </a>
            <form action="${ctx}/logout" method="post">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <button class="button button-quiet" type="submit">Sign out</button>
            </form>
        </div>
    </header>

    <main class="content${wide ? ' content-wide' : ''}" id="main">
        <c:forEach var="f" items="${flashes}">
            <div class="notice${f.error ? ' notice-error' : ' notice-ok'}" role="${f.error ? 'alert' : 'status'}">
                <p><strong><c:out value="${f.error ? 'That didn’t work.' : 'Done.'}"/></strong>&#32;<c:out value="${f.text}"/></p>
                <c:if test="${not empty f.detail}"><p class="notice-detail"><c:out value="${f.detail}"/></p></c:if>
            </div>
        </c:forEach>
        <jsp:doBody/>
    </main>
</div>
</body>
</html>
