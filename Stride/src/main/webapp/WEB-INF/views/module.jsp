<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="${module.code}" active="modules">
    <header class="page-head">
        <p class="muted"><c:out value="${module.code}"/></p>
        <h1><c:out value="${module.name}"/></h1>
        <p class="lead"><c:out value="${module.description}"/></p>
        <p class="muted"><c:out value="Lecturer: ${empty module.lecturerId ? 'not assigned' : module.lecturerName} · ${module.studentCount} students"/></p>
        <c:if test="${teaches}">
            <p class="page-actions"><a class="button" href="${ctx}/notices/new?module=${module.id}">Post a notice to this module</a></p>
        </c:if>
    </header>

    <c:if test="${not empty myAttendance}">
        <p class="summary"><strong><c:out value="Your attendance: ${myAttendance.percent}%"/></strong>&#32;<c:out value="(${myAttendance.attended} of ${myAttendance.total} classes)"/></p>
    </c:if>

    <div class="two-col">
        <section aria-labelledby="slots-title">
            <h2 id="slots-title">Weekly classes</h2>
            <c:choose>
                <c:when test="${empty slots}"><p class="empty-note">No classes on the timetable yet.</p></c:when>
                <c:otherwise>
                    <ul class="plain-list">
                        <c:forEach var="e" items="${slots}">
                            <li><strong><c:out value="${e.dayLabel}"/></strong><c:out value=" ${e.startText} to ${e.endText}, "/><a href="${ctx}/map?room=${e.room}"><c:out value="${e.room}"/></a></li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>

        <section aria-labelledby="res-title">
            <h2 id="res-title">Resources</h2>
            <c:if test="${empty resources}"><p class="empty-note">No resources yet.</p></c:if>
            <ul class="plain-list">
                <c:forEach var="r" items="${resources}">
                    <li><a href="<c:out value='${r.url}'/>" target="_blank" rel="noopener noreferrer"><c:out value="${r.title}"/><span class="visually-hidden"> (opens in a new tab)</span></a>
                        <span class="pill"><c:out value="${r.kind}"/></span>
                        <c:if test="${teaches}">
                            <form action="${ctx}/module/resource/delete" method="post" class="inline-form">
                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                <input type="hidden" name="code" value="${module.code}"><input type="hidden" name="id" value="${r.id}">
                                <button class="link-button" type="submit">Remove</button>
                            </form>
                        </c:if>
                    </li>
                </c:forEach>
            </ul>
        </section>
    </div>

    <section aria-labelledby="asmt-title">
        <h2 id="asmt-title">Assessments</h2>
        <c:choose>
            <c:when test="${empty assessments}"><p class="empty-note">No assessments yet.</p></c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th scope="col">Assessment</th><th scope="col">Kind</th><th scope="col">Due</th><th scope="col">Weight</th><th scope="col">Study hours</th><c:if test="${teaches}"><th scope="col"><span class="visually-hidden">Actions</span></th></c:if></tr></thead>
                        <tbody>
                        <c:forEach var="a" items="${assessments}">
                            <tr>
                                <td><c:out value="${a.title}"/></td>
                                <td><c:out value="${a.kind.label}"/></td>
                                <td><c:out value="${a.dueText}"/></td>
                                <td><c:out value="${a.weight}%"/></td>
                                <td><c:out value="${a.estHours}"/></td>
                                <c:if test="${teaches}">
                                    <td><form action="${ctx}/module/assessment/delete" method="post" class="inline-form">
                                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                        <input type="hidden" name="code" value="${module.code}"><input type="hidden" name="id" value="${a.id}">
                                        <button class="link-button" type="submit">Remove</button></form></td>
                                </c:if>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </section>

    <c:if test="${teaches}">
        <div class="two-col">
            <form class="form-card" action="${ctx}/module/assessment/add" method="post" aria-labelledby="add-a">
                <h2 id="add-a">Add an assessment</h2>
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="code" value="${module.code}">
                <div class="field"><label for="a-title">Title</label><input class="control" id="a-title" name="title" required maxlength="160"></div>
                <div class="field"><label for="a-kind">Kind</label>
                    <select class="control" id="a-kind" name="kind"><c:forEach var="k" items="${kinds}"><option value="${k.name}"><c:out value="${k.label}"/></option></c:forEach></select></div>
                <div class="field"><label for="a-due">Due</label><input class="control" id="a-due" name="due" type="datetime-local" required></div>
                <div class="field"><label for="a-weight">Weight (%)</label><input class="control" id="a-weight" name="weight" type="number" min="0" max="100" required></div>
                <div class="field"><label for="a-hours">Study hours a student needs</label><input class="control" id="a-hours" name="hours" type="number" min="1" max="200">
                    <p class="hint">Leave empty to use the usual amount for this kind of work.</p></div>
                <button class="button" type="submit">Add assessment</button>
            </form>
            <form class="form-card" action="${ctx}/module/resource/add" method="post" aria-labelledby="add-r">
                <h2 id="add-r">Add a resource</h2>
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="code" value="${module.code}">
                <div class="field"><label for="r-title">Title</label><input class="control" id="r-title" name="title" required maxlength="160"></div>
                <div class="field"><label for="r-url">Web address</label><input class="control" id="r-url" name="url" type="url" required placeholder="https://"></div>
                <div class="field"><label for="r-kind">Kind</label><input class="control" id="r-kind" name="kind" placeholder="Reference, Slides, Video" maxlength="12"></div>
                <button class="button" type="submit">Add resource</button>
            </form>
        </div>
    </c:if>

    <section aria-labelledby="mn-title">
        <h2 id="mn-title">Notices for this module</h2>
        <c:if test="${empty notices}"><p class="empty-note">No notices for this module.</p></c:if>
        <ul class="stream">
            <c:forEach var="a" items="${notices}">
                <li class="post">
                    <div class="post-body">
                        <p class="post-meta"><strong><c:out value="${a.author}"/></strong><c:out value=", ${a.postedAtText}"/></p>
                        <h3><c:out value="${a.title}"/></h3>
                        <p class="post-text"><c:out value="${a.body}"/></p>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </section>
</s:shell>
