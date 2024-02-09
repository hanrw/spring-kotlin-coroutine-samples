package org.up.blocking

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

const val MDC_REQUEST_ID = "req-id"

@Component
class RequestIDFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reqId = UUID.randomUUID().toString().replace("-", "").take(10)
        MDC.put(MDC_REQUEST_ID, reqId)


        filterChain.doFilter(request, response)
    }
}