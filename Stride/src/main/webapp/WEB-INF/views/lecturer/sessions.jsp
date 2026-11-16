<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Take attendance" active="sessions">
    <header class="page-head">
        <p class="muted"><c:out value="${today}"/></p>
        <h1>Take attendance</h1>
        <p class="lead">Open check-in for a class. Students scan the QR code or type the code on the screen; it changes every few seconds.</p>
    </header>

    <section aria-labelledby="today-title">
        <h2 id="today-title">Your classes today</h2>
        <c:choose>
            <c:when test="${empty todayClasses}"><p class="empty-note">You have no classes on the timetable today. Use “Extra class” below for a make-up session.</p></c:when>
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
                                <p class="muted"><c:out value="Room: ${k.room}"/></p>
                                <c:choose>
                                    <c:when test="${not empty openByEntry[k.entryId]}">
                                        <a class="button" href="${ctx}/lecturer/session?id=${openByEntry[k.entryId].id}">Check-in is open: manage</a>
                                    </c:when>
                                    <c:when test="${k.done}"><p class="muted">Finished. Open “Recent sessions” to change records.</p></c:when>
                                    <c:otherwise>
                                        <form action="${ctx}/lecturer/open" method="post">
                                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                            <input type="hidden" name="entry" value="${k.entryId}">
                                            <button class="button" type="submit">Open check-in</button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>

    <form class="form-card form-inline" action="${ctx}/lecturer/open-extra" method="post" aria-labelledby="extra-title">
        <h2 id="extra-title">Extra class</h2>
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <div class="field"><label for="module">Module</label>
            <select class="control" id="module" name="module" required>
                <c:forEach var="m" items="${modules}"><option value="${m.id}"><c:out value="${m.code} ${m.name}"/></option></c:forEach>
            </select></div>
        <div class="field"><label for="room">Room</label>
            <select class="control" id="room" name="room">
                <option value="">No room (online)</option>
                <c:forEach var="r" items="${rooms}"><option value="${r.id}"><c:out value="${r.name}"/></option></c:forEach>
            </select></div>
        <button class="button" type="submit">Open check-in</button>
    </form>

    <section aria-labelledby="recent-title">
        <h2 id="recent-title">Recent sessions</h2>
        <c:choose>
            <c:when test="${empty sessions}"><p class="empty-note">No sessions yet.</p></c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th scope="col">Date</th><th scope="col">Module</th><th scope="col">Room</th><th scope="col">Checked in</th><th scope="col">To review</th><th scope="col"><span class="visually-hidden">Open</span></th></tr></thead>
                        <tbody>
                        <c:forEach var="x" items="${sessions}">
                            <tr>
                                <td><c:out value="${x.dateText} ${x.startText}"/></td>
                                <td><c:out value="${x.moduleCode}"/></td>
                                <td><c:out value="${x.roomName}"/></td>
                                <td><c:out value="${x.checkedIn} of ${x.enrolled}"/></td>
                                <td><c:if test="${x.flagged > 0}"><span class="pill pill-dark"><c:out value="${x.flagged} flagged"/></span></c:if></td>
                                <td><a href="${ctx}/lecturer/session?id=${x.id}">Open<span class="visually-hidden"><c:out value=" ${x.moduleCode} ${x.dateText}"/></span></a></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</s:shell>
