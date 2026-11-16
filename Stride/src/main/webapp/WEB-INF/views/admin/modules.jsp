<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Modules" active="admin-modules">
    <header class="page-head">
        <h1>Modules</h1>
        <p class="lead">Modules, their lecturers and how many students are enrolled. Open a module to enrol students.</p>
    </header>
    <div class="table-wrap">
        <table class="table">
            <thead><tr><th scope="col">Code</th><th scope="col">Name</th><th scope="col">Lecturer</th><th scope="col">Students</th></tr></thead>
            <tbody>
            <c:forEach var="m" items="${modules}">
                <tr>
                    <th scope="row"><a href="${ctx}/admin/module?id=${m.id}"><c:out value="${m.code}"/></a></th>
                    <td><c:out value="${m.name}"/></td>
                    <td><c:out value="${empty m.lecturerId ? 'Not assigned' : m.lecturerName}"/></td>
                    <td><c:out value="${m.studentCount}"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>

    <form class="form-card" action="${ctx}/admin/modules/create" method="post" aria-labelledby="new-title">
        <h2 id="new-title">Add a module</h2>
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <div class="form-grid">
            <div class="field"><label for="code">Code</label><input class="control" id="code" name="code" required maxlength="20" placeholder="CSE320"></div>
            <div class="field"><label for="name">Name</label><input class="control" id="name" name="name" required maxlength="120"></div>
            <div class="field"><label for="lecturer">Lecturer</label>
                <select class="control" id="lecturer" name="lecturer"><option value="">Not assigned</option>
                    <c:forEach var="l" items="${lecturers}"><option value="${l.id}"><c:out value="${l.fullName}"/></option></c:forEach></select></div>
        </div>
        <div class="field"><label for="description">Description</label><textarea class="control" id="description" name="description" rows="3" maxlength="600"></textarea></div>
        <button class="button" type="submit">Add module</button>
    </form>
</s:shell>
