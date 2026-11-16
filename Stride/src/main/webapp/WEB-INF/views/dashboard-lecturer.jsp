<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Home" active="home">
    <header class="page-head">
        <p class="muted"><c:out value="${today}"/></p>
        <h1><c:out value="${greeting}, ${sessionScope.strideUser.firstName}."/></h1>
        <p class="lead">Your teaching today, and what needs your attention.</p>
    </header>

    <c:forEach var="o" items="${liveSessions}">
        <a class="banner" href="${ctx}/lecturer/session?id=${o.id}">
            <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-session"/></svg>
            <span><strong><c:out value="${o.moduleCode}"/></strong> check-in is open until <c:out value="${o.closesText}"/>: <c:out value="${o.checkedIn} of ${o.enrolled}"/> checked in.</span>
            <span class="banner-action">Open</span>
        </a>
    </c:forEach>

    <section aria-labelledby="teach-title">
        <h2 id="teach-title">Teaching today</h2>
        <c:choose>
            <c:when test="${empty todayClasses}"><p class="empty-note">You have no classes today.</p></c:when>
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
                                <c:if test="${not k.done}">
                                    <form action="${ctx}/lecturer/open" method="post">
                                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                        <input type="hidden" name="entry" value="${k.entryId}">
                                        <button class="button" type="submit">Take attendance</button>
                                    </form>
                                </c:if>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>

    <div class="glance">
        <section class="card" aria-labelledby="review-title">
            <div class="card-head"><h2 id="review-title">Check-ins to review</h2></div>
            <div class="card-body">
                <p class="big"><c:out value="${flaggedCount} flagged"/></p>
                <p>Low-confidence or shared-device check-ins wait for you to accept or change them.</p>
                <a class="button" href="${ctx}/lecturer/sessions">Review sessions</a>
            </div>
        </section>
        <section class="card" aria-labelledby="mods-title">
            <div class="card-head"><h2 id="mods-title">My modules</h2></div>
            <div class="card-body">
                <ul class="plain-list">
                    <c:forEach var="m" items="${modules}">
                        <li><a href="${ctx}/module?code=${m.code}"><strong><c:out value="${m.code}"/></strong>&#32;<c:out value="${m.name}"/></a>
                            <span class="muted"><c:out value="(${m.studentCount} students)"/></span></li>
                    </c:forEach>
                </ul>
                <a class="button button-quiet" href="${ctx}/notices/new">Post a notice</a>
            </div>
        </section>
        <section class="card" aria-labelledby="rep-title">
            <div class="card-head"><h2 id="rep-title">Reports</h2></div>
            <div class="card-body">
                <p>Attendance by module, students at risk, check-in assurance, room use and the weekly trend.</p>
                <a class="button" href="${ctx}/reports">Open reports</a>
            </div>
        </section>
        <section class="card" aria-labelledby="notice-title">
            <div class="card-head"><h2 id="notice-title">Latest notice</h2></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty latestNotice}"><p class="muted">No notices yet.</p></c:when>
                    <c:otherwise>
                        <p class="big"><c:out value="${latestNotice.title}"/></p>
                        <p class="muted"><c:out value="From ${latestNotice.author}, ${latestNotice.postedOn}"/></p>
                    </c:otherwise>
                </c:choose>
                <a class="button button-quiet" href="${ctx}/notices">All notices</a>
            </div>
        </section>
    </div>
</s:shell>
