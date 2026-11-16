<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<s:shell title="Check in" active="checkin">
    <header class="page-head">
        <h1>Check in to class</h1>
        <p class="lead">Scan the QR code on the screen with your phone camera, or type the code below.</p>
    </header>

    <c:if test="${not empty result}">
        <c:choose>
            <c:when test="${result.ok}">
                <section class="result result-ok" role="status" aria-labelledby="ok-title">
                    <svg class="ico ico-big" aria-hidden="true" focusable="false"><use href="#i-attendance"/></svg>
                    <div>
                        <h2 id="ok-title"><c:out value="${result.message}"/></h2>
                        <p class="big"><c:out value="${result.session.moduleCode} ${result.session.moduleName}"/></p>
                        <p><c:out value="Status: ${result.status.label}. Confidence: ${result.confidenceLabel} (${result.confidence} out of 100)."/></p>
                        <details>
                            <summary>How Stride checked this</summary>
                            <ul class="plain-list">
                                <c:forEach var="f" items="${result.factors}"><li><c:out value="${f}"/></li></c:forEach>
                            </ul>
                        </details>
                        <a class="button" href="${ctx}/attendance">See my attendance</a>
                    </div>
                </section>
            </c:when>
            <c:otherwise>
                <div class="notice notice-error" role="alert"><p><strong>Not checked in.</strong>&#32;<c:out value="${result.message}"/></p></div>
            </c:otherwise>
        </c:choose>
    </c:if>

    <c:if test="${empty result or not result.ok}">
        <c:choose>
            <c:when test="${not empty qrSession}">
                <form class="form-card checkin-form" action="${ctx}/checkin" method="post" data-location="${locationOn ? 'on' : 'off'}">
                    <h2><c:out value="${qrSession.moduleCode} ${qrSession.moduleName}"/></h2>
                    <p class="muted"><c:out value="${qrSession.roomName}, started ${qrSession.startText}"/></p>
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="s" value="${qrSession.id}">
                    <input type="hidden" name="t" value="<c:out value='${qrToken}'/>">
                    <input type="hidden" name="lat"><input type="hidden" name="lon"><input type="hidden" name="acc">
                    <button class="button button-big" type="submit">Check me in</button>
                    <p class="hint"><c:out value="Tap within ${codeSeconds} seconds of scanning. If it has expired, scan the screen again."/></p>
                </form>
            </c:when>
            <c:otherwise>
                <form class="form-card checkin-form" action="${ctx}/checkin" method="post" data-location="${locationOn ? 'on' : 'off'}">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="lat"><input type="hidden" name="lon"><input type="hidden" name="acc">
                    <div class="field">
                        <label for="code">Code on the screen</label>
                        <input class="control code-input" id="code" name="code" required autocomplete="off" autocapitalize="characters"
                               spellcheck="false" maxlength="9" placeholder="ABC123" aria-describedby="code-hint" autofocus>
                        <p class="hint" id="code-hint"><c:out value="Six letters and numbers. The code changes every ${codeSeconds} seconds, so type the one showing now."/></p>
                    </div>
                    <button class="button button-big" type="submit">Check in</button>
                </form>
            </c:otherwise>
        </c:choose>
    </c:if>

    <section aria-labelledby="open-title">
        <h2 id="open-title">Taking attendance now</h2>
        <c:choose>
            <c:when test="${empty openSessions}"><p class="empty-note">None of your classes is taking attendance right now.</p></c:when>
            <c:otherwise>
                <ul class="plain-list">
                    <c:forEach var="o" items="${openSessions}">
                        <li><strong><c:out value="${o.moduleCode}"/></strong>&#32;<c:out value="${o.moduleName}, ${o.roomName}. Open until ${o.closesText}."/></li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
        <c:if test="${locationOn}"><p class="muted small">Your browser may ask to share your location. It is used once, only to confirm you are on campus, and is not stored.</p></c:if>
    </section>
    <script src="${ctx}/assets/js/checkin.js" defer></script>
</s:shell>
