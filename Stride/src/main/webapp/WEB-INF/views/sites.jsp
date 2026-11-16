<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="College sites" active="${sessionScope.strideUser.administrator ? 'admin-sites' : 'sites'}">
    <header class="page-head">
        <h1><c:out value="${empty query ? 'College sites' : 'Search results'}"/></h1>
        <p class="lead">
            <c:choose>
                <c:when test="${not empty query}">
                    <c:out value="${services.size()} of ${total} sites match “${query}”."/>&#32;<a href="${ctx}/sites">Show all</a>
                </c:when>
                <c:otherwise>All the college’s websites in one list. Each one opens in a new tab.</c:otherwise>
            </c:choose>
        </p>
    </header>

    <c:if test="${not empty moduleHits}">
        <section aria-labelledby="mods-title">
            <h2 id="mods-title">Your modules</h2>
            <ul class="plain-list">
                <c:forEach var="m" items="${moduleHits}">
                    <li><a href="${ctx}/module?code=${m.code}"><strong><c:out value="${m.code}"/></strong>&#32;<c:out value="${m.name}"/></a></li>
                </c:forEach>
            </ul>
        </section>
    </c:if>

    <c:choose>
        <c:when test="${empty services}">
            <p class="empty-note"><c:out value="No site matches “${query}”. Try a simpler word, such as results, courses, timetable, books or apply."/>
                <a href="${ctx}/sites">Show all sites</a></p>
        </c:when>
        <c:otherwise>
            <div class="tiles tiles-wide">
                <c:forEach var="t" items="${services}">
                    <a class="tile" href="<c:out value='${t.url}'/>" target="_blank" rel="noopener noreferrer">
                        <span class="tile-icon"><svg class="ico" aria-hidden="true" focusable="false"><use href="#i-${t.id}"/></svg></span>
                        <span class="tile-title"><c:out value="${t.name}"/></span>
                        <span class="tile-sub"><c:out value="${t.description}"/></span>
                        <span class="tile-open"><c:out value="Open ${t.name}"/>
                            <span class="visually-hidden">(opens in a new tab)</span>
                            <svg class="ico" aria-hidden="true" focusable="false"><use href="#i-external"/></svg></span>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</s:shell>
