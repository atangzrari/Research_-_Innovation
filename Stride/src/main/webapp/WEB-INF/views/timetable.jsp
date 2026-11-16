<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Timetable" active="${sessionScope.strideUser.student ? 'classes' : 'timetable'}">
    <header class="page-head">
        <h1><c:out value="${sessionScope.strideUser.student ? 'My week' : 'My timetable'}"/></h1>
        <p class="lead">Your classes for each day of the week.</p>
        <p class="page-actions">
            <a class="button" href="${ctx}/timetable.ics"><svg class="ico" aria-hidden="true" focusable="false"><use href="#i-download"/></svg>Add to my calendar (.ics)</a>
        </p>
    </header>

    <div class="week">
        <c:forEach var="d" items="${week}">
            <section class="day${d.key.name() == todayName ? ' day-today' : ''}" aria-labelledby="d-${d.key}">
                <h2 id="d-${d.key}"><c:out value="${d.key.name().charAt(0)}${fn:toLowerCase(d.key.name().substring(1))}"/>
                    <c:if test="${d.key.name() == todayName}"><span class="pill">Today</span></c:if></h2>
                <c:choose>
                    <c:when test="${empty d.value}"><p class="muted">No classes.</p></c:when>
                    <c:otherwise>
                        <ul class="slots">
                            <c:forEach var="e" items="${d.value}">
                                <li>
                                    <p class="slot-time"><c:out value="${e.startText} to ${e.endText}"/></p>
                                    <p><a href="${ctx}/module?code=${e.moduleCode}"><strong><c:out value="${e.moduleCode}"/></strong></a>&#32;<c:out value="${e.moduleName}"/></p>
                                    <p class="muted"><c:out value="${e.room}"/></p>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:forEach>
    </div>
</s:shell>
