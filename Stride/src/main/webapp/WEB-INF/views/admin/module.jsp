<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="${m.code}" active="admin-modules">
    <header class="page-head">
        <p class="muted"><a href="${ctx}/admin/modules">Modules</a></p>
        <h1><c:out value="${m.code} ${m.name}"/></h1>
        <p class="lead"><c:out value="${students.size()} students enrolled."/>&#32;<a href="${ctx}/module?code=${m.code}">Open the module page</a></p>
    </header>

    <div class="two-col">
        <form class="form-card" action="${ctx}/admin/modules/update" method="post" aria-labelledby="edit-title">
            <h2 id="edit-title">Details</h2>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${m.id}">
            <div class="field"><label for="name">Name</label><input class="control" id="name" name="name" required value="<c:out value='${m.name}'/>"></div>
            <div class="field"><label for="lecturer">Lecturer</label>
                <select class="control" id="lecturer" name="lecturer"><option value="">Not assigned</option>
                    <c:forEach var="l" items="${lecturers}"><option value="${l.id}" ${m.lecturerId == l.id ? 'selected' : ''}><c:out value="${l.fullName}"/></option></c:forEach></select></div>
            <div class="field"><label for="description">Description</label><textarea class="control" id="description" name="description" rows="4"><c:out value="${m.description}"/></textarea></div>
            <button class="button" type="submit">Save</button>
        </form>

        <form class="form-card" action="${ctx}/admin/modules/enrol" method="post" aria-labelledby="enrol-title">
            <h2 id="enrol-title">Enrol students</h2>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${m.id}">
            <div class="field"><label for="students">Student numbers or e-mails</label>
                <textarea class="control" id="students" name="students" rows="6" required placeholder="CSE24-901&#10;tumelo.molefe@stride.test"></textarea>
                <p class="hint">One per line, or separated by commas. You can paste a column from a spreadsheet.</p></div>
            <button class="button" type="submit">Enrol</button>
        </form>
    </div>

    <section aria-labelledby="list-title">
        <h2 id="list-title">Enrolled students</h2>
        <div class="table-wrap">
            <table class="table">
                <thead><tr><th scope="col">Student</th><th scope="col">Number</th><th scope="col">E-mail</th><th scope="col"><span class="visually-hidden">Remove</span></th></tr></thead>
                <tbody>
                <c:forEach var="st" items="${students}">
                    <tr>
                        <th scope="row"><c:out value="${st.fullName}"/></th>
                        <td><c:out value="${st.studentNumber}"/></td>
                        <td><c:out value="${st.email}"/></td>
                        <td><form action="${ctx}/admin/modules/unenrol" method="post" class="inline-form">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${m.id}"><input type="hidden" name="student" value="${st.id}">
                            <button class="link-button" type="submit">Remove<span class="visually-hidden"><c:out value=" ${st.fullName}"/></span></button></form></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </section>
</s:shell>
