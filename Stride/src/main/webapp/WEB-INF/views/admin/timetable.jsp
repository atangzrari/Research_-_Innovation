<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Timetable" active="admin-timetable">
    <header class="page-head">
        <h1>Timetable</h1>
        <p class="lead">Every weekly class. New classes are checked for clashes: room, lecturer, students who share modules, opening hours and room size.</p>
    </header>

    <form class="form-card" id="add" action="${ctx}/admin/timetable/add" method="post" aria-labelledby="add-title">
        <h2 id="add-title">Add a class</h2>
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <div class="form-grid">
            <div class="field"><label for="module">Module</label>
                <select class="control" id="module" name="module"><c:forEach var="m" items="${modules}"><option value="${m.id}"><c:out value="${m.code} ${m.name}"/></option></c:forEach></select></div>
            <div class="field"><label for="room">Room</label>
                <select class="control" id="room" name="room"><c:forEach var="r" items="${rooms}"><option value="${r.id}"><c:out value="${r.name} (${r.capacity})"/></option></c:forEach></select></div>
            <div class="field"><label for="day">Day</label>
                <select class="control" id="day" name="day"><c:forEach var="d" items="${days}"><option value="${d}"><c:out value="${d}"/></option></c:forEach></select></div>
            <div class="field"><label for="start">Starts</label><input class="control" type="time" id="start" name="start" required step="900"></div>
            <div class="field"><label for="end">Ends</label><input class="control" type="time" id="end" name="end" required step="900"></div>
        </div>
        <button class="button" type="submit">Check and add</button>
    </form>

    <div class="week">
        <c:forEach var="d" items="${byDay}">
            <section class="day" aria-labelledby="d-${d.key}">
                <h2 id="d-${d.key}"><c:out value="${d.key}"/></h2>
                <ul class="slots">
                    <c:forEach var="e" items="${d.value}">
                        <li>
                            <p class="slot-time"><c:out value="${e.startText} to ${e.endText}"/></p>
                            <p><strong><c:out value="${e.moduleCode}"/></strong><c:out value=" in ${e.room}"/></p>
                            <form action="${ctx}/admin/timetable/remove" method="post" class="inline-form">
                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${e.id}">
                                <button class="link-button" type="submit">Remove<span class="visually-hidden"><c:out value=" ${e.moduleCode} ${d.key} ${e.startText}"/></span></button>
                            </form>
                        </li>
                    </c:forEach>
                </ul>
            </section>
        </c:forEach>
    </div>
</s:shell>
