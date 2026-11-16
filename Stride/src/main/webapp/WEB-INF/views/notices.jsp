<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Notices" active="notices">
    <header class="page-head">
        <h1>Notices</h1>
        <p class="lead">Messages from the college and your lecturers, newest first.</p>
        <div class="page-actions">
            <c:if test="${unreadCount > 0}">
                <form action="${ctx}/notices/read" method="post">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <button class="button button-quiet" type="submit"><c:out value="Mark ${unreadCount} as read"/></button>
                </form>
            </c:if>
            <c:if test="${sessionScope.strideUser.staff}"><a class="button" href="${ctx}/notices/new">Post a notice</a></c:if>
        </div>
    </header>

    <c:if test="${empty announcements}"><p class="empty-note">No notices yet.</p></c:if>
    <ul class="stream">
        <c:forEach var="a" items="${announcements}">
            <li class="post${a.read ? '' : ' post-unread'}">
                <p class="avatar avatar-big" aria-hidden="true"><c:out value="${a.authorInitial}"/></p>
                <div class="post-body">
                    <p class="post-meta"><strong><c:out value="${a.author}"/></strong><c:out value=", ${a.postedAtText} · For: ${a.audience}"/>
                        <c:if test="${a.pinned}"> <span class="pill">Pinned</span></c:if>
                        <c:if test="${not a.read}"> <span class="pill pill-dark">New</span></c:if></p>
                    <h2><c:out value="${a.title}"/></h2>
                    <p class="post-text"><c:out value="${a.body}"/></p>
                    <c:if test="${sessionScope.strideUser.administrator or a.author == sessionScope.strideUser.fullName}">
                        <form action="${ctx}/notices/delete" method="post" class="inline-form">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                            <input type="hidden" name="id" value="${a.id}">
                            <button class="link-button" type="submit">Remove this notice</button>
                        </form>
                    </c:if>
                </div>
            </li>
        </c:forEach>
    </ul>
</s:shell>
