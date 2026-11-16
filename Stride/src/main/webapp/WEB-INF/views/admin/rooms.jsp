<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Rooms" active="rooms">
    <header class="page-head">
        <h1>Rooms</h1>
        <p class="lead">Buildings and the rooms in them. Rooms appear in the free-room finder and on the campus map.</p>
    </header>
    <ul class="building-list">
        <c:forEach var="b" items="${buildings}">
            <li class="building">
                <h2><c:out value="${b.name}"/></h2>
                <p class="muted"><c:out value="${b.description}"/></p>
                <c:choose>
                    <c:when test="${empty b.rooms}"><p class="muted">No bookable rooms.</p></c:when>
                    <c:otherwise>
                        <ul class="rooms-inline"><c:forEach var="r" items="${b.rooms}"><li><strong><c:out value="${r.name}"/></strong><c:out value=", ${r.capacity} seats"/></li></c:forEach></ul>
                    </c:otherwise>
                </c:choose>
            </li>
        </c:forEach>
    </ul>
    <form class="form-card" action="${ctx}/admin/rooms/add" method="post" aria-labelledby="add-title">
        <h2 id="add-title">Add a room</h2>
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <div class="form-grid">
            <div class="field"><label for="name">Name</label><input class="control" id="name" name="name" required maxlength="60" placeholder="Block B 206"></div>
            <div class="field"><label for="building">Building</label>
                <select class="control" id="building" name="building"><c:forEach var="b" items="${buildings}"><option value="${b.id}"><c:out value="${b.name}"/></option></c:forEach></select></div>
            <div class="field"><label for="capacity">Seats</label><input class="control" type="number" id="capacity" name="capacity" min="1" max="2000" required></div>
        </div>
        <button class="button" type="submit">Add room</button>
    </form>
</s:shell>
