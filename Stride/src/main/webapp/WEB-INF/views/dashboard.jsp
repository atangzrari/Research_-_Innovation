<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Home" active="home">
    <header class="page-head">
        <p class="muted"><c:out value="${today}"/></p>
        <h1><c:out value="${greeting}, ${sessionScope.strideUser.firstName}."/></h1>
        <p class="lead">What would you like to do today?</p>
    </header>

    <c:forEach var="o" items="${openSessions}">
        <a class="banner" href="${ctx}/checkin">
            <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-checkin"/></svg>
            <span><strong><c:out value="${o.moduleCode} ${o.moduleName}"/></strong> is taking attendance now, in <c:out value="${o.roomName}"/>.</span>
            <span class="banner-action">Check in</span>
        </a>
    </c:forEach>

    <section aria-labelledby="do-title">
        <h2 id="do-title" class="visually-hidden">College sites</h2>
        <div class="tiles">
            <c:forEach var="t" items="${tiles}">
                <a class="tile" href="<c:out value='${t.url}'/>" target="_blank" rel="noopener noreferrer">
                    <span class="tile-icon"><svg class="ico" aria-hidden="true" focusable="false"><use href="#i-${t.id}"/></svg></span>
                    <span class="tile-title"><c:out value="${t.task}"/></span>
                    <span class="tile-sub">Opens <c:out value="${t.name}"/> in a new tab
                        <span class="visually-hidden">(opens in a new tab)</span></span>
                </a>
            </c:forEach>
        </div>
    </section>

    <div class="glance">
        <section class="card" aria-labelledby="glance-title">
            <div class="card-head">
                <h2 id="glance-title">Today at a glance</h2>
            </div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${classCount == 0}">
                        <p class="big">No classes today.</p>
                        <p class="muted">Enjoy the day.</p>
                    </c:when>
                    <c:otherwise>
                        <p class="big"><c:out value="You have ${classCount} ${classCount == 1 ? 'class' : 'classes'} today."/></p>
                        <c:if test="${not empty nextClass}">
                            <p><c:out value="${nextClass.now ? 'On now: ' : 'Next: '}"/><strong><c:out value="${nextClass.moduleName}"/></strong><c:out value=" at ${nextClass.startText} in ${nextClass.room}."/></p>
                        </c:if>
                        <c:if test="${empty nextClass}"><p class="muted">All of today’s classes are finished.</p></c:if>
                    </c:otherwise>
                </c:choose>
                <a class="button" href="${ctx}/classes">See my classes</a>
            </div>
        </section>

        <section class="card" aria-labelledby="notice-title">
            <div class="card-head">
                <h2 id="notice-title">Latest notice</h2>
                <c:if test="${unreadCount > 0}"><p class="pill"><c:out value="${unreadCount} unread"/></p></c:if>
            </div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty latestNotice}"><p class="muted">No notices yet.</p></c:when>
                    <c:otherwise>
                        <p class="big"><c:out value="${latestNotice.title}"/></p>
                        <p><c:out value="${latestNotice.body}"/></p>
                        <p class="muted"><c:out value="From ${latestNotice.author}, ${latestNotice.postedOn}"/></p>
                    </c:otherwise>
                </c:choose>
                <a class="button" href="${ctx}/notices">Read all notices</a>
            </div>
        </section>

        <section class="card" aria-labelledby="deadline-title">
            <div class="card-head"><h2 id="deadline-title">Next deadline</h2></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty nextDeadline}"><p class="muted">Nothing due in the next three weeks.</p></c:when>
                    <c:otherwise>
                        <p class="big"><c:out value="${nextDeadline.moduleCode} ${nextDeadline.title}"/></p>
                        <p><c:out value="${nextDeadline.kind.label}, ${nextDeadline.weight}% of the module. Due ${nextDeadline.dueText}."/></p>
                    </c:otherwise>
                </c:choose>
                <a class="button" href="${ctx}/planner">Open my study plan</a>
            </div>
        </section>

        <section class="card" aria-labelledby="att-title">
            <div class="card-head"><h2 id="att-title">Attendance to watch</h2></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty lowest}"><p class="muted">No classes have been held yet.</p></c:when>
                    <c:otherwise>
                        <p class="big"><c:out value="${lowest.moduleCode}: ${lowest.percent}%"/></p>
                        <p><c:out value="${lowest.attended} of ${lowest.total} classes in ${lowest.moduleName}."/>
                            <c:if test="${lowest.percent < riskPercent}"><strong><c:out value=" Below ${riskPercent}%."/></strong></c:if></p>
                    </c:otherwise>
                </c:choose>
                <a class="button" href="${ctx}/attendance">See all attendance</a>
            </div>
        </section>
    </div>
</s:shell>
