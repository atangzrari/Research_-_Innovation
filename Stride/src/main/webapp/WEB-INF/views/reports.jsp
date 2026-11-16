<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Reports" active="reports">
    <header class="page-head">
        <h1>Reports</h1>
        <p class="lead"><c:out value="${sessionScope.strideUser.administrator ? 'Covering every module.' : 'Covering the modules you teach.'}"/> Each report has a chart, a table and a spreadsheet download.</p>
    </header>
    <ul class="tiles tiles-wide">
        <c:forEach var="r" items="${catalogue}">
            <li><a class="tile" href="${ctx}/report?id=${r.id}">
                <span class="tile-icon"><svg class="ico" aria-hidden="true" focusable="false"><use href="#i-reports"/></svg></span>
                <span class="tile-title"><c:out value="${r.title}"/></span>
                <span class="tile-sub"><c:out value="${r.purpose}"/></span>
                <span class="tile-open"><c:out value="${fn:toUpperCase(r.id)}"/></span>
            </a></li>
        </c:forEach>
    </ul>
</s:shell>
