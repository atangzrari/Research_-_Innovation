<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="chart" required="true" type="com.stride.report.Chart" %>
<%@ attribute name="id" required="true" %>
<%-- A bar or line chart drawn as plain SVG from geometry computed on the server.
     The same numbers are always in a table next to it, so the chart is never the only source. --%>
<figure class="chart">
    <svg viewBox="${chart.viewBox}" role="img" aria-labelledby="${id}-t" preserveAspectRatio="xMidYMid meet">
        <title id="${id}-t"><c:out value="${chart.title}"/>. The same figures are in the table below.</title>
        <c:forEach var="t" items="${chart.ticks}">
            <line class="grid" x1="${chart.left}" x2="${chart.right}" y1="${t.y}" y2="${t.y}"/>
            <text class="tick" x="${chart.left - 8}" y="${t.textY}" text-anchor="end"><c:out value="${t.label}"/></text>
        </c:forEach>
        <line class="axis" x1="${chart.left}" x2="${chart.right}" y1="${chart.baseY}" y2="${chart.baseY}"/>
        <c:if test="${not empty chart.lineY}">
            <line class="guide" x1="${chart.left}" x2="${chart.right}" y1="${chart.lineY}" y2="${chart.lineY}"/>
            <text class="guide-label" x="${chart.right}" y="${chart.lineY - 6}" text-anchor="end"><c:out value="${chart.lineLabel}"/></text>
        </c:if>
        <c:choose>
            <c:when test="${chart.line}">
                <polyline class="trend" points="${chart.points}"/>
                <c:forEach var="b" items="${chart.bars}">
                    <rect class="dot${b.alert ? ' alert' : ''}" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}"/>
                    <text class="value" x="${b.cx}" y="${b.valueY - 4}" text-anchor="middle"><c:out value="${b.valueText}"/></text>
                    <text class="label" x="${b.cx}" y="${b.labelY}" text-anchor="middle"><c:out value="${b.label}"/></text>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <c:forEach var="b" items="${chart.bars}">
                    <rect class="bar${b.alert ? ' alert' : ''}" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}"/>
                    <text class="value" x="${b.cx}" y="${b.valueY}" text-anchor="middle"><c:out value="${b.valueText}"/></text>
                    <text class="label" x="${b.cx}" y="${b.labelY}" text-anchor="middle"><c:out value="${b.label}"/></text>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </svg>
    <figcaption><c:out value="${chart.title}"/><c:if test="${not empty chart.lineLabel}">. Outlined (white) bars are below the <c:out value="${chart.lineLabel}"/>.</c:if></figcaption>
</figure>
