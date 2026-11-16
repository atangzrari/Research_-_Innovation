<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Study planner" active="planner">
    <header class="page-head">
        <h1>Study planner</h1>
        <p class="lead">Study hours for your upcoming deadlines, fitted around your classes and spread out before each due date.</p>
        <p class="page-actions">
            <a class="button" href="${ctx}/planner.ics?${fn:escapeXml(query)}"><svg class="ico" aria-hidden="true" focusable="false"><use href="#i-download"/></svg>Add plan to my calendar (.ics)</a>
        </p>
    </header>

    <form class="prefs" action="${ctx}/planner" method="get" aria-label="Planning preferences">
        <div class="field"><label for="from">Study from</label><input class="control" type="time" id="from" name="from" step="3600" value="${plan.preferences.fromText}"></div>
        <div class="field"><label for="to">Until</label><input class="control" type="time" id="to" name="to" step="3600" value="${plan.preferences.toText}"></div>
        <div class="field"><label for="hours">Most hours a day</label><input class="control" type="number" id="hours" name="hours" min="1" max="8" value="${plan.preferences.maxHoursPerDay}"></div>
        <div class="field check"><input type="checkbox" id="sundays" name="sundays" ${plan.preferences.sundays ? 'checked' : ''}><label for="sundays">Plan on Sundays</label></div>
        <button class="button" type="submit">Update plan</button>
    </form>

    <c:forEach var="w" items="${plan.warnings}">
        <div class="notice notice-error" role="alert"><p><c:out value="${w}"/></p></div>
    </c:forEach>

    <section aria-labelledby="items-title">
        <h2 id="items-title">Deadlines in the next three weeks</h2>
        <c:choose>
            <c:when test="${empty plan.items}"><p class="empty-note">No deadlines in the next three weeks. Nothing to plan.</p></c:when>
            <c:otherwise>
                <ul class="course-cards">
                    <c:forEach var="i" items="${plan.items}">
                        <li class="course-card${i.shortfall > 0 ? ' course-risk' : ''}">
                            <p class="course-code"><c:out value="${i.assessment.moduleCode} · ${i.assessment.kind.label} · ${i.assessment.weight}%"/></p>
                            <h3><c:out value="${i.assessment.title}"/></h3>
                            <p><c:out value="Due ${i.assessment.dueText} (in ${i.daysLeft} ${i.daysLeft == 1 ? 'day' : 'days'})"/></p>
                            <progress value="${i.planned}" max="${i.needed}" aria-label="Hours planned for ${i.assessment.title}"><c:out value="${i.percent}%"/></progress>
                            <p><c:out value="${i.planned} of ${i.needed} hours planned"/><c:if test="${i.shortfall > 0}"><strong><c:out value=", ${i.shortfall} short"/></strong></c:if></p>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>

    <section aria-labelledby="days-title">
        <h2 id="days-title"><c:out value="Your plan: ${plan.totalHours} study hours"/></h2>
        <div class="plan-days">
            <c:forEach var="d" items="${plan.busyDays}">
                <section class="plan-day" aria-label="${d.label}">
                    <h3><c:out value="${d.label}"/></h3>
                    <ul class="slots">
                        <c:forEach var="b" items="${d.blocks}">
                            <li class="${b.study ? 'slot-study' : 'slot-class'}">
                                <p class="slot-time"><c:out value="${b.startText} to ${b.endText}"/></p>
                                <p><c:out value="${b.study ? 'Study: ' : ''}"/><strong><c:out value="${b.moduleCode}"/></strong>&#32;<c:out value="${b.title}"/></p>
                            </li>
                        </c:forEach>
                    </ul>
                </section>
            </c:forEach>
        </div>
        <p class="muted small">How it plans: earliest deadline first; whole hours that do not clash with a class; at most two hours a day on one piece of work; nothing on the day it is due.</p>
    </section>
</s:shell>
