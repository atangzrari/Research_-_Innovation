<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Audit log" active="audit">
    <header class="page-head">
        <h1>Audit log</h1>
        <p class="lead">Sign-ins, attendance corrections and administrative changes, newest first. Entries cannot be edited.</p>
    </header>
    <form class="prefs" action="${ctx}/admin/audit" method="get" aria-label="Filter the audit log">
        <div class="field"><label for="action">Action</label>
            <select class="control" id="action" name="action"><option value="">All actions</option>
                <c:forEach var="a" items="${actions}"><option value="${a}" ${actionFilter == a ? 'selected' : ''}><c:out value="${a}"/></option></c:forEach></select></div>
        <button class="button" type="submit">Filter</button>
    </form>
    <div class="table-wrap">
        <table class="table">
            <thead><tr><th scope="col">When</th><th scope="col">Who</th><th scope="col">Action</th><th scope="col">Detail</th><th scope="col">Address</th></tr></thead>
            <tbody>
            <c:forEach var="e" items="${entries}">
                <tr><td><c:out value="${e.atText}"/></td><td><c:out value="${e.actorName}"/></td><td><c:out value="${e.action}"/></td><td><c:out value="${e.detail}"/></td><td><c:out value="${e.clientIp}"/></td></tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</s:shell>
