<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="${u.fullName}" active="users">
    <header class="page-head">
        <p class="muted"><a href="${ctx}/admin/users">People</a></p>
        <h1><c:out value="${u.fullName}"/></h1>
        <p class="lead"><c:out value="${u.role.label}, ${u.active ? 'active' : 'switched off'}"/><c:if test="${u.mustChangePassword}"><c:out value=", must choose a new password"/></c:if>.</p>
    </header>

    <div class="two-col">
        <form class="form-card" action="${ctx}/admin/users/update" method="post" aria-labelledby="edit-title">
            <h2 id="edit-title">Details</h2>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${u.id}">
            <div class="field"><label for="first">First name</label><input class="control" id="first" name="first" required value="<c:out value='${u.firstName}'/>"></div>
            <div class="field"><label for="last">Last name</label><input class="control" id="last" name="last" required value="<c:out value='${u.lastName}'/>"></div>
            <div class="field"><label for="email">E-mail</label><input class="control" type="email" id="email" name="email" required value="<c:out value='${u.email}'/>"></div>
            <div class="field"><label for="role">Role</label>
                <select class="control" id="role" name="role"><c:forEach var="r" items="${roles}"><option value="${r.name}" ${u.role == r ? 'selected' : ''}><c:out value="${r.label}"/></option></c:forEach></select></div>
            <div class="field"><label for="number">Student number</label><input class="control" id="number" name="number" value="<c:out value='${u.studentNumber}'/>"></div>
            <button class="button" type="submit">Save</button>
        </form>

        <section class="card" aria-labelledby="acc-title">
            <div class="card-head"><h2 id="acc-title">Access</h2></div>
            <div class="card-body">
                <p><c:out value="Modules: "/><c:forEach var="m" items="${modules}" varStatus="st"><c:out value="${m.code}${st.last ? '' : ', '}"/></c:forEach><c:if test="${empty modules}">none</c:if></p>
                <p><c:out value="Last sign-in: ${empty u.lastLoginAt ? 'never' : u.lastLoginAt}"/></p>
                <form action="${ctx}/admin/users/reset" method="post">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${u.id}">
                    <button class="button button-quiet" type="submit">Reset password</button>
                </form>
                <form action="${ctx}/admin/users/active" method="post">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="id" value="${u.id}">
                    <input type="hidden" name="active" value="${u.active ? 'false' : 'true'}">
                    <button class="button button-quiet" type="submit"><c:out value="${u.active ? 'Switch account off' : 'Switch account on'}"/></button>
                </form>
                <p class="muted small">Switching an account off keeps its records but stops it signing in. Accounts are never deleted, so attendance history stays complete.</p>
            </div>
        </section>
    </div>
</s:shell>
