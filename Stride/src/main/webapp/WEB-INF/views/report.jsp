<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="${report.title}" active="reports" wide="true">
    <header class="page-head">
        <p class="muted"><a href="${ctx}/reports">Reports</a><c:out value=" · ${report.scope}"/></p>
        <h1><c:out value="${report.title}"/></h1>
        <p class="lead"><c:out value="${report.purpose}"/></p>
        <p class="page-actions">
            <a class="button" href="${ctx}/report?id=${report.id}&amp;format=csv"><svg class="ico" aria-hidden="true" focusable="false"><use href="#i-download"/></svg>Download for a spreadsheet (CSV)</a>
            <button class="button button-quiet" type="button" data-print>Print</button>
        </p>
    </header>

    <c:if test="${not empty report.chart}"><s:chart chart="${report.chart}" id="ch-${report.id}"/></c:if>

    <div class="table-wrap">
        <table class="table">
            <caption><c:out value="${report.title}"/>: the figures</caption>
            <thead><tr><c:forEach var="h" items="${report.columns}"><th scope="col"><c:out value="${h}"/></th></c:forEach></tr></thead>
            <tbody>
            <c:if test="${empty report.rows}"><tr><td colspan="${report.columns.size()}">No data yet.</td></tr></c:if>
            <c:forEach var="row" items="${report.rows}">
                <tr><c:forEach var="cell" items="${row}"><td><c:out value="${cell}"/></td></c:forEach></tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    <ul class="notes"><c:forEach var="n" items="${report.notes}"><li><c:out value="${n}"/></li></c:forEach></ul>

    <nav class="report-nav" aria-label="Other reports">
        <c:forEach var="r" items="${catalogue}">
            <a class="chip${r.id == report.id ? ' chip-on' : ''}" href="${ctx}/report?id=${r.id}" ${r.id == report.id ? 'aria-current="page"' : ''}><c:out value="${r.title}"/></a>
        </c:forEach>
    </nav>
</s:shell>
