<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="College sites" active="admin-sites">
    <header class="page-head">
        <h1>College sites</h1>
        <p class="lead">The five sites on every student’s home page. Fix an address here and it changes for everyone at once.</p>
        <p class="page-actions"><a class="button button-quiet" href="${ctx}/sites">See them as students do</a></p>
    </header>
    <c:forEach var="t" items="${sites}">
        <form class="form-card" action="${ctx}/admin/sites/update" method="post" aria-labelledby="h-${t.id}">
            <h2 id="h-${t.id}"><c:out value="${t.name}"/></h2>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="slug" value="${t.id}">
            <div class="form-grid">
                <div class="field"><label for="n-${t.id}">Name</label><input class="control" id="n-${t.id}" name="name" required value="<c:out value='${t.name}'/>"></div>
                <div class="field"><label for="k-${t.id}">Button text</label><input class="control" id="k-${t.id}" name="task" required value="<c:out value='${t.task}'/>"></div>
            </div>
            <div class="field"><label for="u-${t.id}">Address</label><input class="control" type="url" id="u-${t.id}" name="url" required value="<c:out value='${t.url}'/>"></div>
            <div class="field"><label for="d-${t.id}">Description</label><input class="control" id="d-${t.id}" name="description" required value="<c:out value='${t.description}'/>"></div>
            <div class="field"><label for="w-${t.id}">Search words</label><input class="control" id="w-${t.id}" name="keywords" value="<c:out value='${t.keywords}'/>"></div>
            <button class="button" type="submit">Save</button>
            <a class="small-link" href="<c:out value='${t.url}'/>" target="_blank" rel="noopener noreferrer">Test the address<span class="visually-hidden"> (opens in a new tab)</span></a>
        </form>
    </c:forEach>
</s:shell>
