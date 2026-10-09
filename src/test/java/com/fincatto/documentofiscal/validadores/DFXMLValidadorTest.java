package com.fincatto.documentofiscal.validadores;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

public class DFXMLValidadorTest {

    /**
     * Regressao: os includes do XSD precisam ser resolvidos pelo classloader.
     * <br>
     * Quando eram resolvidos reabrindo a URL do recurso, um jar aninhado sob caminho com acento
     * fazia o include falhar em silencio e a validacao acusava "src-resolve" no tipo TNFe.
     * <br>
     * Este teste roda com os schemas soltos em disco, entao ele nao reproduz o cenario do jar
     * aninhado. O que ele garante e que o mecanismo de resolucao continua montando a gramatica
     * completa: o erro esperado e de conteudo do XML, nunca de tipo nao resolvido.
     */
    @Test
    public void deveResolverOsTiposGlobaisDosIncludesDoSchemaDaNFe400() throws Exception {
        try {
            DFXMLValidador.validaNota400("<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"/>");
            Assert.fail("XML incompleto deveria ser rejeitado pelo schema");
        } catch (final SAXException e) {
            Assert.assertFalse(e.getMessage(), e.getMessage().contains("src-resolve"));
        }
    }
}
