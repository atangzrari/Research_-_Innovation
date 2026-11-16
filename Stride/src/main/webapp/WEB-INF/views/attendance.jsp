<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Attendance" active="attendance">
    <header class="page-head">
        <h1>Attendance</h1>
        <p class="lead">How many classes you have been to in each subject.</p>
        <p class="page-actions"><a class="button" href="${ctx}/checkin">Check in to a class</a></p>
    </header>

    <p class="summary"><strong><c:out value="Overall: ${overall.percent}%"/></strong>&#32;<c:out value="(${overall.attended} of ${overall.total} classes). Below ${riskPercent}% in a subject puts you at risk."/></p>

    <ul class="course-cards">
        <c:forEach var="t" items="${attendance}">
            <li class="course-card${t.percent < riskPercent and t.total > 0 ? ' course-risk' : ''}">
                <p class="course-code"><c:out value="${t.moduleCode}"/></p>
                <h2><c:out value="${t.moduleName}"/></h2>
                <p class="percent"><c:out value="${t.percent}%"/></p>
                <progress value="${t.attended}" max="${t.total == 0 ? 1 : t.total}"
                          aria-label="Attendance for ${t.moduleName}"><c:out value="${t.percent}%"/></progress>
                <p>You went to <strong><c:out value="${t.attended} of ${t.total}"/></strong> classes.</p>
                <c:if test="${t.percent < riskPercent and t.total > 0}"><p class="warn">Below <c:out value="${riskPercent}"/>%: try not to miss the next classes.</p></c:if>
            </li>
        </c:forEach>
    </ul>

    <section aria-labelledby="hist-title">
        <h2 id="hist-title">Recent check-ins</h2>
        <c:choose>
            <c:when test="${empty history}"><p class="empty-note">No check-ins yet.</p></c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <caption class="visually-hidden">Your most recent check-ins</caption>
                        <thead><tr><th scope="col">When</th><th scope="col">Status</th><th scope="col">How</th><th scope="col">Confidence</th><th scope="col">Note</th></tr></thead>
                        <tbody>
                        <c:forEach var="h" items="${history}">
                            <tr>
                                <td><c:out value="${h.checkedInAt.toLocalDate()} ${h.timeText}"/></td>
                                <td><c:out value="${h.status.label}"/></td>
                                <td><c:out value="${h.methodLabel}"/></td>
                                <td><c:out value="${h.confidenceLabel} (${h.confidence})"/><c:if test="${h.flagged}"> <span class="pill">Under review</span></c:if></td>
                                <td><c:out value="${h.correctionReason}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</s:shell>
