<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="My modules" active="modules">
    <header class="page-head">
        <h1>My modules</h1>
        <p class="lead"><c:out value="${sessionScope.strideUser.student ? 'The modules you are enrolled in.' : 'The modules you teach.'}"/> Open one for its classes, deadlines, resources and notices.</p>
    </header>
    <c:if test="${empty modules}"><p class="empty-note">No modules yet.</p></c:if>
    <ul class="course-cards">
        <c:forEach var="m" items="${modules}">
            <li class="course-card">
                <p class="course-code"><c:out value="${m.code}"/></p>
                <h2><a href="${ctx}/module?code=${m.code}"><c:out value="${m.name}"/></a></h2>
                <p><c:out value="${m.description}"/></p>
                <p class="muted"><c:out value="Lecturer: ${empty m.lecturerId ? 'not assigned' : m.lecturerName} · ${m.studentCount} students"/></p>
            </li>
        </c:forEach>
    </ul>
</s:shell>
