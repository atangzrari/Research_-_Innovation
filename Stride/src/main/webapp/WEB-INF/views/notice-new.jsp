<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Post a notice" active="notices">
    <header class="page-head">
        <h1>Post a notice</h1>
        <p class="lead"><c:out value="${sessionScope.strideUser.lecturer ? 'Students enrolled in the module you choose will see it under Notices.' : 'Choose a module, or send it to everyone.'}"/></p>
    </header>

    <form class="form-card" action="${ctx}/notices/new" method="post">
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <div class="field">
            <label for="module">Who is it for?</label>
            <select class="control" id="module" name="module" ${sessionScope.strideUser.lecturer ? 'required' : ''}>
                <c:if test="${sessionScope.strideUser.administrator}"><option value="">Everyone</option></c:if>
                <c:forEach var="m" items="${modules}">
                    <option value="${m.id}" ${preselect == m.id ? 'selected' : ''}><c:out value="${m.code} ${m.name}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="field">
            <label for="title">Title</label>
            <input class="control" id="title" name="title" maxlength="160" required>
        </div>
        <div class="field">
            <label for="body">Message</label>
            <textarea class="control" id="body" name="body" rows="7" maxlength="4000" required></textarea>
            <p class="hint">Plain text. Keep it short and say what students need to do.</p>
        </div>
        <c:if test="${sessionScope.strideUser.administrator}">
            <div class="field check">
                <input type="checkbox" id="pinned" name="pinned">
                <label for="pinned">Pin to the top of everyone’s notices</label>
            </div>
        </c:if>
        <button class="button" type="submit">Post notice</button>
        <a class="button button-quiet" href="${ctx}/notices">Cancel</a>
    </form>
</s:shell>
