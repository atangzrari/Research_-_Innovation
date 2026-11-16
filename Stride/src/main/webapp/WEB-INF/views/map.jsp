<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Campus map" active="map">
    <header class="page-head">
        <h1>Campus map</h1>
        <p class="lead">Buildings, rooms and which rooms are free right now. Choose a building for details.</p>
        <p class="muted small">Schematic sample layout, not to scale.</p>
    </header>

    <figure class="campus">
        <svg viewBox="0 0 760 420" role="img" aria-labelledby="map-t">
            <title id="map-t">Schematic campus map. The same information is in the list of buildings below.</title>
            <rect class="ground" x="0" y="0" width="760" height="420"/>
            <path class="walk" d="M20 210 H740 M340 20 V400"/>
            <c:forEach var="b" items="${buildings}">
                <a href="#${b.anchor}">
                    <rect class="bldg${b.freeNow > 0 ? ' bldg-free' : ''}${b.name == highlightBuilding ? ' bldg-hit' : ''}" x="${b.mapX}" y="${b.mapY}" width="${b.mapW}" height="${b.mapH}"/>
                    <text class="bldg-name" x="${b.labelX}" y="${b.labelY - 4}" text-anchor="middle"><c:out value="${b.name}"/></text>
                    <c:if test="${not empty b.rooms}">
                        <text class="bldg-free-text" x="${b.labelX}" y="${b.labelY + 18}" text-anchor="middle"><c:out value="${b.freeNow} of ${b.rooms.size()} free"/></text>
                    </c:if>
                </a>
            </c:forEach>
        </svg>
        <figcaption>Buildings with a free room now are drawn with a thick border.</figcaption>
    </figure>

    <ul class="building-list">
        <c:forEach var="b" items="${buildings}">
            <li id="${b.anchor}" class="building${b.name == highlightBuilding ? ' building-hit' : ''}">
                <h2><c:out value="${b.name}"/>&#32;<span class="muted">(<c:out value="${b.code}"/>)</span></h2>
                <p><c:out value="${b.description}"/></p>
                <c:if test="${not empty b.accessNote}"><p class="access"><strong>Access:</strong>&#32;<c:out value="${b.accessNote}"/></p></c:if>
                <c:if test="${not empty b.rooms}">
                    <ul class="rooms-inline">
                        <c:forEach var="r" items="${b.rooms}">
                            <li class="${r.name == highlight ? 'room-hit' : ''}"><strong><c:out value="${r.name}"/></strong><c:out value=", ${r.capacity} seats: "/>
                                <c:choose>
                                    <c:when test="${not empty free[r.name]}"><c:out value="free until ${free[r.name].freeUntil}"/></c:when>
                                    <c:otherwise>in use or closed</c:otherwise>
                                </c:choose></li>
                        </c:forEach>
                    </ul>
                </c:if>
            </li>
        </c:forEach>
    </ul>
</s:shell>
