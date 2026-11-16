<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Home" active="home">
    <header class="page-head">
        <p class="muted"><c:out value="${today}"/></p>
        <h1><c:out value="${greeting}, ${sessionScope.strideUser.firstName}."/></h1>
        <p class="lead">Stride at a glance.</p>
    </header>

    <ul class="stats">
        <li><a href="${ctx}/admin/users?role=STUDENT"><span class="stat"><c:out value="${counts['STUDENT']}"/></span> active students</a></li>
        <li><a href="${ctx}/admin/users?role=LECTURER"><span class="stat"><c:out value="${counts['LECTURER']}"/></span> lecturers</a></li>
        <li><a href="${ctx}/admin/modules"><span class="stat"><c:out value="${moduleCount}"/></span> modules</a></li>
        <li><a href="${ctx}/admin/timetable"><span class="stat"><c:out value="${slotCount}"/></span> weekly classes</a></li>
        <li><a href="${ctx}/admin/rooms"><span class="stat"><c:out value="${roomCount}"/></span> rooms, <c:out value="${freeNow}"/> free now</a></li>
    </ul>

    <div class="glance">
        <section class="card" aria-labelledby="todo-title">
            <div class="card-head"><h2 id="todo-title">Common tasks</h2></div>
            <div class="card-body">
                <ul class="plain-list">
                    <li><a href="${ctx}/admin/users#new">Create an account</a></li>
                    <li><a href="${ctx}/admin/modules">Enrol students in a module</a></li>
                    <li><a href="${ctx}/admin/timetable#add">Add a class to the timetable</a></li>
                    <li><a href="${ctx}/notices/new">Post a notice to everyone</a></li>
                    <li><a href="${ctx}/admin/sites">Fix a college site address</a></li>
                </ul>
            </div>
        </section>
        <section class="card" aria-labelledby="audit-title">
            <div class="card-head"><h2 id="audit-title">Recent activity</h2></div>
            <div class="card-body">
                <ul class="plain-list small">
                    <c:forEach var="a" items="${recentAudit}">
                        <li><span class="muted"><c:out value="${a.atText}"/></span>&#32;<strong><c:out value="${a.action}"/></strong>&#32;<c:out value="${a.actorName}"/></li>
                    </c:forEach>
                </ul>
                <a class="button button-quiet" href="${ctx}/admin/audit">Full audit log</a>
            </div>
        </section>
    </div>
</s:shell>
