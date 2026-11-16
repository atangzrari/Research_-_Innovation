<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="My account" active="account">
    <c:set var="me" value="${sessionScope.strideUser}"/>
    <header class="page-head">
        <h1>My account</h1>
        <p class="lead"><c:out value="${me.fullName}, ${me.role.label}"/></p>
    </header>

    <c:if test="${forced}">
        <div class="notice notice-error" role="alert"><p><strong>Choose a new password.</strong>&#32;An administrator gave you a temporary password. Choose your own before you continue.</p></div>
    </c:if>

    <div class="two-col">
        <section class="card" aria-labelledby="det-title">
            <div class="card-head"><h2 id="det-title">Details</h2></div>
            <div class="card-body">
                <dl class="details">
                    <dt>E-mail</dt><dd><c:out value="${me.email}"/></dd>
                    <c:if test="${not empty me.studentNumber}"><dt>Student number</dt><dd><c:out value="${me.studentNumber}"/></dd></c:if>
                    <dt>Role</dt><dd><c:out value="${me.role.label}"/></dd>
                    <dt>Modules</dt><dd><c:forEach var="m" items="${modules}" varStatus="st"><c:out value="${m.code}${st.last ? '' : ', '}"/></c:forEach><c:if test="${empty modules}">None</c:if></dd>
                </dl>
                <p class="muted small">To change your name or e-mail, ask an administrator.</p>
            </div>
        </section>

        <form class="form-card" action="${ctx}/account/password" method="post" aria-labelledby="pw-title">
            <h2 id="pw-title">Change password</h2>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <div class="field"><label for="current">Current password</label>
                <input class="control" type="password" id="current" name="current" required autocomplete="current-password"></div>
            <div class="field"><label for="next">New password</label>
                <input class="control" type="password" id="next" name="next" required minlength="10" maxlength="128" autocomplete="new-password" aria-describedby="pw-hint">
                <p class="hint" id="pw-hint">At least 10 characters. A short sentence you will remember works well.</p></div>
            <div class="field"><label for="repeat">New password again</label>
                <input class="control" type="password" id="repeat" name="repeat" required minlength="10" maxlength="128" autocomplete="new-password"></div>
            <button class="button" type="submit">Change password</button>
        </form>
    </div>
</s:shell>
