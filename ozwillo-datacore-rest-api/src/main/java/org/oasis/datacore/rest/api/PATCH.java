package org.oasis.datacore.rest.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.HttpMethod;

/**
 * HTTP PATCH method.
 * Allows to only sends diffs, where PUT is more "provide and update the full resource state"
 * See http://stackoverflow.com/questions/17897171/how-to-have-a-patch-annotation-in-jax-rs
 * 
 * @author mdutoo
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PATCH") // makes it usable by JAXRS runtimes
public @interface PATCH {
}