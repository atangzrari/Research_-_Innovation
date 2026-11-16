<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Assistant" active="assistant">
    <header class="page-head">
        <h1>Study assistant</h1>
        <p class="lead">Ask about your classes, attendance, deadlines, notices or rooms. Answers come only from your own Stride information.</p>
    </header>

    <div class="chat" aria-live="polite">
        <c:if test="${empty chat}">
            <p class="empty-note">Try one of these:</p>
        </c:if>
        <c:forEach var="t" items="${chat}" varStatus="st">
            <div class="msg msg-me"${st.last ? ' id="latest"' : ''}><p><c:out value="${t.question}"/></p></div>
            <div class="msg msg-bot">
                <c:forEach var="line" items="${t.answer}"><p><c:out value="${line}"/></p></c:forEach>
                <c:if test="${not empty t.source}"><p class="source"><c:out value="From: ${t.source}"/></p></c:if>
            </div>
        </c:forEach>
    </div>

    <form class="ask" action="${ctx}/assistant" method="post">
        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
        <label class="visually-hidden" for="q">Your question</label>
        <input class="control" id="q" name="q" maxlength="300" autocomplete="off" placeholder="For example: when is my next deadline?" required>
        <button class="button" type="submit">Ask</button>
    </form>

    <c:set var="sugs" value="${empty chat ? starters : chat[chat.size() - 1].suggestions}"/>
    <div class="suggestions" role="group" aria-label="Suggested questions">
        <c:forEach var="sq" items="${sugs}">
            <form action="${ctx}/assistant" method="post" class="inline-form">
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="q" value="<c:out value='${sq}'/>">
                <button class="chip" type="submit"><c:out value="${sq}"/></button>
            </form>
        </c:forEach>
    </div>

    <c:if test="${not empty chat}">
        <form action="${ctx}/assistant" method="post" class="inline-form">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="action" value="clear">
            <button class="link-button" type="submit">Clear conversation</button>
        </form>
    </c:if>
</s:shell>
