<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="My classes" active="classes">
    <header class="page-head">
        <p class="muted"><c:out value="${today}"/></p>
        <h1><c:out value="${sessionScope.strideUser.student ? 'My classes' : 'Classes and free rooms'}"/></h1>
        <p class="lead">Everything on today, in time order, and the rooms you can use right now.</p>
        <p class="page-actions"><a class="button button-quiet" href="${ctx}/timetable">Whole week</a></p>
    </header>

    <section aria-labelledby="today-title">
        <h2 id="today-title">Today</h2>
        <c:choose>
            <c:when test="${empty todayClasses}">
                <p class="empty-note">No classes today.</p>
            </c:when>
            <c:otherwise>
                <ul class="class-cards">
                    <c:forEach var="k" items="${todayClasses}">
                        <li class="class-card${k.now ? ' class-now' : ''}">
                            <div class="class-top">
                                <p class="class-code"><c:out value="${k.moduleCode}"/></p>
                                <p class="badge"><c:out value="${k.statusLabel}"/></p>
                            </div>
                            <div class="class-body">
                                <h3><c:out value="${k.moduleName}"/></h3>
                                <p class="class-time"><c:out value="${k.startText} to ${k.endText}"/></p>
                                <p class="muted">Room: <a href="${ctx}/map?room=${k.room}"><c:out value="${k.room}"/></a></p>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>

    <section aria-labelledby="rooms-title">
        <h2 id="rooms-title">Rooms you can use right now</h2>
        <c:choose>
            <c:when test="${not roomsOpen}"><p class="empty-note"><c:out value="Rooms are listed between ${openText}."/></p></c:when>
            <c:when test="${empty freeRooms}"><p class="empty-note">Every room is in use right now.</p></c:when>
            <c:otherwise>
                <ul class="room-list">
                    <c:forEach var="r" items="${freeRooms}">
                        <li>
                            <p class="room-name"><c:out value="${r.name}"/></p>
                            <p class="muted"><c:out value="${r.building}, seats ${r.capacity}"/></p>
                            <p><strong><c:out value="Free until ${r.freeUntil}"/></strong></p>
                            <a class="small-link" href="${ctx}/map?room=${r.name}">Show on map</a>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>
</s:shell>
