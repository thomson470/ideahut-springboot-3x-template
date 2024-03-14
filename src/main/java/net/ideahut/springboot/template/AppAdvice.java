package net.ideahut.springboot.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.context.RequestContext;
import net.ideahut.springboot.exception.ResultException;
import net.ideahut.springboot.exception.ResultRuntimeException;
import net.ideahut.springboot.object.Result;
import net.ideahut.springboot.util.ErrorUtil;

@Slf4j
@ControllerAdvice
public class AppAdvice implements ResponseBodyAdvice<Object> {
	
	@Autowired
	private AppProperties appProperties;
	
	@ExceptionHandler
    @ResponseStatus(code = HttpStatus.OK)
    @ResponseBody
    public Result handleAllException(Throwable throwable) {
		if (Boolean.TRUE.equals(appProperties.getLoggingError())) {
    		log.error(AppAdvice.class.getSimpleName(), throwable);
    	}
    	Throwable ex = ErrorUtil.getCause(throwable);
    	Result result = null;
    	if (ex instanceof ResultException) {
    		result = ((ResultException)ex).getResult();
    	} else if (ex instanceof ResultRuntimeException) {
    		result = ((ResultRuntimeException)ex).getResult();
    	} else {
    		result = Result.error(ErrorUtil.getErrors(ex, true));
    	}
    	return result;
    }

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(
		Object body, 
		MethodParameter returnType, 
		MediaType selectedContentType,
		Class<? extends HttpMessageConverter<?>> selectedConverterType, 
		ServerHttpRequest request,
		ServerHttpResponse response
	) {
		RequestContext.destroy();
		if (!(body instanceof Result)) {
			return Result.success(body);
		}
		return body;
	}
    
}