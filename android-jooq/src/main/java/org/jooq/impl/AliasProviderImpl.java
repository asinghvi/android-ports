/**
 * Copyright (c) 2009-2012, Lukas Eder, lukas.eder@gmail.com
 * All rights reserved.
 *
 * This software is licensed to you under the Apache License, Version 2.0
 * (the "License"); You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * . Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * . Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * . Neither the name "jOOQ" nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.jooq.impl;

import static org.jooq.SQLDialect.DERBY;
import static org.jooq.SQLDialect.HSQLDB;
import static org.jooq.SQLDialect.MYSQL;
import static org.jooq.SQLDialect.POSTGRES;

import java.util.Arrays;
import java.util.List;

import org.jooq.AliasProvider;
import org.jooq.Attachable;
import org.jooq.BindContext;
import org.jooq.RenderContext;

/**
 * @author Lukas Eder
 */
class AliasProviderImpl<T extends AliasProvider<T>> extends AbstractNamedQueryPart implements AliasProvider<T> {

    private static final long serialVersionUID = -2456848365524191614L;
    private final T           aliasProvider;
    private final String      alias;
    private final boolean     wrapInParentheses;

    AliasProviderImpl(T aliasProvider, String alias) {
        this(aliasProvider, alias, false);
    }

    AliasProviderImpl(T aliasProvider, String alias, boolean wrapInParentheses) {
        super(alias);

        this.aliasProvider = aliasProvider;
        this.alias = alias;
        this.wrapInParentheses = wrapInParentheses;
    }

    @Override
    public final List<Attachable> getAttachables() {
        return getAttachables(aliasProvider);
    }

    protected final T getAliasProvider() {
        return aliasProvider;
    }

    @Override
    public final void toSQL(RenderContext context) {
        if (context.declareFields() || context.declareTables()) {
            if (wrapInParentheses) {
                context.sql("(");
            }

            context.sql(aliasProvider);

            if (wrapInParentheses) {
                context.sql(")");
            }

            // [#291] some aliases cause trouble, if they are not explicitly marked using "as"
            if (Arrays.asList(DERBY, HSQLDB, MYSQL, POSTGRES).contains(context.getDialect())) {
                context.keyword(" as");
            }

            context.sql(" ");
            context.literal(alias);

            // [#756] If the aliased object is an anonymous table (usually an
            // unnested array), then field names must be part of the alias
            // declaration. For example:
            //
            // SELECT t.column_value FROM UNNEST(ARRAY[1, 2]) AS t(column_value)

            // TODO: Is this still needed?

            switch (context.getDialect()) {
                case HSQLDB:
                case POSTGRES: {
                    // The javac compiler doesn't like casting of generics
                    Object o = aliasProvider;

                    if (context.declareTables() && o instanceof ArrayTable) {
                        ArrayTable table = (ArrayTable) o;

                        context.sql("(");
                        Util.toSQLNames(context, table.getFields());
                        context.sql(")");
                    }

                    break;
                }
            }
        }
        else {
            context.literal(alias);
        }
    }

    @Override
    public final void bind(BindContext context) {
        if (context.declareFields() || context.declareTables()) {
            context.bind(aliasProvider);
        }
        else {
            // Don't bind any values
        }
    }

    @Override
    public final T as(String as) {
        return aliasProvider.as(as);
    }

    @Override
    public final boolean declaresFields() {
        return true;
    }

    @Override
    public final boolean declaresTables() {
        return true;
    }
}
