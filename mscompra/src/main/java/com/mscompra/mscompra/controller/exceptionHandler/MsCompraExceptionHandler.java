package com.mscompra.mscompra.controller.exceptionHandler;


import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.PropertyBindingException;
import com.mscompra.mscompra.service.exception.EntidadeNaoEncontradaException;
import com.mscompra.mscompra.service.exception.NegocioException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class MsCompraExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String MSG_ERRO_GENERICA_USUARIO_FINAL
            = "Ocorreu um erro interno inesperado no sistema. Tente novamente e se "
            + "o problema persistir, entre em contato com o administrador do sistema.";

    @Autowired
    MessageSource messageSource;

    @Override //Exce????o lan??ada quando o manipulador de solicita????o n??o pode gerar uma resposta aceit??vel pelo cliente.
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                      HttpHeaders headers, HttpStatus status,
                                                                      WebRequest request) {
        return ResponseEntity.status(status).headers(headers).build();
    }

    @Override //Lan??ado quando os erros de liga????o s??o considerados fatais
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers,
                                                         HttpStatus status,
                                                         WebRequest request) {

        return handleValidationInternal(ex, headers, status, request, ex.getBindingResult());
    }

    @Override //Exce????o a ser lan??ada quando a valida????o em um argumento anotado com @Valid falha
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {

        return handleValidationInternal(ex, headers, status, request, ex.getBindingResult());
    }

    private ResponseEntity<Object> handleValidationInternal(Exception ex, HttpHeaders headers,
                                                            HttpStatus status, WebRequest request,
                                                            BindingResult bindingResult) {
        EStandardErrorType eStandardErrorType =  EStandardErrorType.DADOS_INVALIDOS;
        String detail = "Um ou mais campos est??o inv??lidos. Fa??a o preenchimento correto e tente novamente.";

        List<StandardError.Object> problemObjects = bindingResult.getAllErrors().stream()
                .map(objectError -> {
                    String message = messageSource.getMessage(objectError, LocaleContextHolder.getLocale());

                    String name = objectError.getObjectName();

                    if (objectError instanceof FieldError) {
                        name = ((FieldError) objectError).getField();
                    }

                    return StandardError.Object.builder()
                            .name(name)
                            .userMessage(message)
                            .build();
                })
                .collect(Collectors.toList());

        StandardError standardError = createStandardErrorBuilder(status, eStandardErrorType, detail)
                .userMessage(detail)
                .objects(problemObjects)
                .build();

        return handleExceptionInternal(ex, standardError, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncaught(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        EStandardErrorType eStandardErrorType = EStandardErrorType.ERRO_DE_SISTEMA;
        String detail = MSG_ERRO_GENERICA_USUARIO_FINAL;

        log.error(ex.getMessage());

        StandardError standardError = createStandardErrorBuilder(status, eStandardErrorType, detail)
                .userMessage(detail)
                .build();

        return handleExceptionInternal(ex, standardError, new HttpHeaders(), status, request);
    }

    @Override //Por padr??o, quando o DispatcherServlet n??o consegue encontrar um manipulador para uma solicita????o,
    // ele envia uma resposta 404
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                                   HttpHeaders headers, HttpStatus status,
                                                                   WebRequest request) {

        EStandardErrorType standardErrorType = EStandardErrorType.RECURSO_NAO_ENCONTRADO;
        String detail = String.format("O recurso %s, que voc?? tentou acessar, ?? inexistente.",
                ex.getRequestURL());

        StandardError standardError = createStandardErrorBuilder(status, standardErrorType, detail)
                .userMessage(MSG_ERRO_GENERICA_USUARIO_FINAL)
                .build();

        return handleExceptionInternal(ex, standardError, headers, status, request);
    }

    @Override //Exce????o lan??ada em uma incompatibilidade de tipo
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatus status, WebRequest request) {

        if (ex instanceof MethodArgumentTypeMismatchException) {
            return handleMethodArgumentTypeMismatch(
                    (MethodArgumentTypeMismatchException) ex, headers, status, request);
        }

        return super.handleTypeMismatch(ex, headers, status, request);
    }

    private ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        EStandardErrorType eStandardErrorType = EStandardErrorType.PARAMETRO_INVALIDO;

        String detail = String.format("O par??metro de URL '%s' recebeu o valor '%s', "
                        + "que ?? de um tipo inv??lido. Corrija e informe um valor compat??vel com o tipo %s.",
                ex.getName(), ex.getValue(), Objects.requireNonNull(ex.getRequiredType()).getSimpleName());

        StandardError standardError = createStandardErrorBuilder(status, eStandardErrorType, detail)
                .userMessage(MSG_ERRO_GENERICA_USUARIO_FINAL)
                .build();

        return handleExceptionInternal(ex, standardError, headers, status, request);
    }

    @Override //Exce????o de mensagem HTTP n??o leg??vel
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        Throwable rootCause = ExceptionUtils.getRootCause(ex);

        if (rootCause instanceof InvalidFormatException) {
            return handleInvalidFormat((InvalidFormatException) rootCause, headers, status, request);
        } else if (rootCause instanceof PropertyBindingException) {
            return handlePropertyBinding((PropertyBindingException) rootCause, headers, status, request);
        }

        EStandardErrorType eStandardErrorType = EStandardErrorType.MENSAGEM_INCOMPREENSIVEL;
        String detail = "O corpo da requisi????o est?? inv??lido. Verifique erro de sintaxe.";

        StandardError standardError = createStandardErrorBuilder(status, eStandardErrorType, detail)
                .userMessage(MSG_ERRO_GENERICA_USUARIO_FINAL)
                .build();

        return handleExceptionInternal(ex, standardError, headers, status, request);
    }

    //erro JSON
    private ResponseEntity<Object> handlePropertyBinding(PropertyBindingException ex,
                                                         HttpHeaders headers, HttpStatus status, WebRequest request) {

        String path = joinPath(ex.getPath());

        EStandardErrorType problemType = EStandardErrorType.MENSAGEM_INCOMPREENSIVEL;
        String detail = String.format("A propriedade '%s' n??o existe. "
                + "Corrija ou remova essa propriedade e tente novamente.", path);

        StandardError standardError = createStandardErrorBuilder(status, problemType, detail)
                .userMessage(MSG_ERRO_GENERICA_USUARIO_FINAL)
                .build();

        return handleExceptionInternal(ex, standardError, headers, status, request);
    }

    // m?? formata????o de um valor para desserializar no JSON
    private ResponseEntity<Object> handleInvalidFormat(InvalidFormatException ex,
                                                       HttpHeaders headers, HttpStatus status, WebRequest request) {

        String path = joinPath(ex.getPath());

        EStandardErrorType eStandardErrorType = EStandardErrorType.MENSAGEM_INCOMPREENSIVEL;
        String detail = String.format("A propriedade '%s' recebeu o valor '%s', "
                        + "que ?? de um tipo inv??lido. Corrija e informe um valor compat??vel com o tipo %s.",
                path, ex.getValue(), ex.getTargetType().getSimpleName());

        StandardError standardError = createStandardErrorBuilder(status, eStandardErrorType, detail)
                .userMessage(MSG_ERRO_GENERICA_USUARIO_FINAL)
                .build();

        return handleExceptionInternal(ex, standardError, headers, status, request);
    }

    @ExceptionHandler(EntidadeNaoEncontradaException.class)
    public ResponseEntity<Object> handleEntidadeNaoEncontrada(EntidadeNaoEncontradaException ex,
                                                              WebRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;
        EStandardErrorType eStandardErrorType = EStandardErrorType.RECURSO_NAO_ENCONTRADO;
        String detail = ex.getMessage();

        StandardError standardError = createStandardErrorBuilder(status, eStandardErrorType, detail)
                .userMessage(detail)
                .build();

        return handleExceptionInternal(ex, standardError, new HttpHeaders(), status, request);
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Object> handleNegocio(NegocioException ex, WebRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        EStandardErrorType eStandardErrorType = EStandardErrorType.ERRO_NEGOCIO;
        String detail = ex.getMessage();

        StandardError standardError = createStandardErrorBuilder(status, eStandardErrorType, detail)
                .userMessage(detail)
                .build();

        return handleExceptionInternal(ex, standardError, new HttpHeaders(), status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatus status, WebRequest request) {

        if (body == null) {
            body = StandardError.builder()
                    .timestamp(OffsetDateTime.now())
                    .title(status.getReasonPhrase())
                    .status(status.value())
                    .userMessage(MSG_ERRO_GENERICA_USUARIO_FINAL)
                    .build();
        } else if (body instanceof String) {
            body = StandardError.builder()
                    .timestamp(OffsetDateTime.now())
                    .title((String) body)
                    .status(status.value())
                    .userMessage(MSG_ERRO_GENERICA_USUARIO_FINAL)
                    .build();
        }

        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    private StandardError.StandardErrorBuilder createStandardErrorBuilder(HttpStatus status,
                                                                    EStandardErrorType eStandardErrorType, String detail) {

        return StandardError.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .type(eStandardErrorType.getUri())
                .title(eStandardErrorType.getTitle())
                .detail(detail);
    }

    private String joinPath(List<JsonMappingException.Reference> references) {
        return references.stream()
                .map(JsonMappingException.Reference::getFieldName)
                .collect(Collectors.joining("."));
    }


}