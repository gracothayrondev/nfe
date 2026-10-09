package com.fincatto.documentofiscal.validadores;

import java.io.InputStream;
import java.io.Reader;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * Resolve os includes e imports dos schemas XSD pelo classloader, em vez de deixar o parser
 * reabrir a URL do recurso.
 * <br><br>
 * <b>Por que essa classe existe</b>
 * <br><br>
 * Um schema da NF-e nao e um arquivo unico. O nfe_v4.00.xsd apenas declara
 * {@code <xs:include schemaLocation="leiauteNFe_v4.00.xsd"/>} e usa o tipo TNFe, que mora no
 * arquivo incluido. Por padrao o parser resolve esse schemaLocation de forma relativa ao
 * systemId do schema principal, monta uma nova URL em texto e reabre essa URL.
 * <br><br>
 * Essa reabertura e o ponto fragil. Quando a lib esta empacotada em um jar aninhado (Spring
 * Boot, com o jar da lib dentro de BOOT-INF/lib do jar da aplicacao) o endereco fica assim:
 * <br><br>
 * <pre>
 * jar:file:/C:/Users/Chocolandia/.../aplicacao.jar!/BOOT-INF/lib/nfe.jar!/schemas/PL_.../nfe_v4.00.xsd
 * </pre>
 * <br>
 * Se o caminho do jar externo contiver acento, a reabertura falha. O espaco e codificado como
 * %20 mas o caractere acentuado fica cru, porque java.net.URI aceita caracteres nao ASCII no
 * caminho sem codificar. O handler de jar aninhado entao nao consegue converter esse texto de
 * volta em um arquivo no disco.
 * <br><br>
 * O agravante e que o parser trata include que nao abre como aviso, nao como erro fatal. Ele
 * segue com a gramatica incompleta e a falha so aparece uma linha depois, na forma de
 * "src-resolve: nao e possivel resolver o nome 'TNFe' para um componente 'type definition'".
 * A mensagem aponta o sintoma, nunca a causa.
 * <br><br>
 * O classloader nao tem esse problema, porque foi ele quem montou o classpath e sabe abrir jar
 * aninhado sem converter caminho em texto. Ao fornecer o conteudo ja aberto, nenhuma URL e
 * remontada e a falha desaparece, junto com a sensibilidade a espaco e ao aninhamento de jar.
 * <br><br>
 * <b>Premissa</b>
 * <br><br>
 * Todos os schemaLocation dos pacotes oficiais da SEFAZ sao nomes de arquivo simples, sem
 * subpasta. Por isso basta procurar o mesmo nome dentro da pasta do pacote. Se algum pacote
 * futuro passar a usar subpastas, {@link #caminhoDoRecurso(String, String)} precisa mudar.
 */
final class DFSchemaResourceResolver implements LSResourceResolver {

    private final String pacote;

    /**
     * @param pacote nome da pasta do pacote de schemas dentro de schemas/, por exemplo
     *               PL_010e_NT2025.002_v1.01
     */
    DFSchemaResourceResolver(final String pacote) {
        this.pacote = pacote;
    }

    /**
     * Chamado pelo parser antes de ele tentar abrir cada include ou import.
     *
     * @param systemId valor literal do schemaLocation, como escrito no arquivo XSD
     * @return o recurso ja aberto, ou null para devolver o controle ao comportamento padrao do
     *         parser. Devolver null quando o recurso nao existe evita mascarar um erro de
     *         configuracao com um include vazio.
     */
    @Override
    public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {
        if (systemId == null) {
            return null;
        }
        final String recurso = DFSchemaResourceResolver.caminhoDoRecurso(this.pacote, systemId);
        final InputStream conteudo = DFSchemaResourceResolver.class.getClassLoader().getResourceAsStream(recurso);
        return conteudo == null ? null : new ClasspathLSInput(publicId, recurso, conteudo);
    }

    /**
     * Monta o caminho do recurso no classpath a partir do nome do arquivo referenciado.
     * <br>
     * O systemId pode chegar como nome simples (leiauteNFe_v4.00.xsd) ou ja expandido pelo
     * parser em uma URL absoluta. Ficar apenas com o trecho apos a ultima barra cobre os dois
     * casos.
     */
    static String caminhoDoRecurso(final String pacote, final String systemId) {
        final String nomeArquivo = systemId.substring(systemId.lastIndexOf('/') + 1);
        return String.format("schemas/%s/%s", pacote, nomeArquivo);
    }

    /**
     * Implementacao minima de {@link LSInput}, o contrato que o parser espera como resposta.
     * <br><br>
     * E privada e estatica de proposito. Privada porque ninguem fora do resolver precisa
     * construir um LSInput, entao ela nao vira API nem publica nem interna. Estatica porque nao
     * usa nada do resolver e assim nao carrega uma referencia implicita a ele.
     * <br><br>
     * A interface tem dezesseis metodos porque foi desenhada para ser generica e mutavel. Aqui
     * so o fluxo de bytes importa: pela especificacao ele tem precedencia sobre o systemId, e e
     * justamente isso que impede o parser de reabrir a URL.
     */
    private static final class ClasspathLSInput implements LSInput {

        private final String publicId;
        private final String systemId;
        private InputStream byteStream;
        private Reader characterStream;
        private String stringData;
        private String baseURI;
        private String encoding;
        private boolean certifiedText;

        ClasspathLSInput(final String publicId, final String systemId, final InputStream byteStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.byteStream = byteStream;
        }

        @Override
        public InputStream getByteStream() {
            return this.byteStream;
        }

        @Override
        public void setByteStream(final InputStream byteStream) {
            this.byteStream = byteStream;
        }

        @Override
        public String getSystemId() {
            return this.systemId;
        }

        @Override
        public String getPublicId() {
            return this.publicId;
        }

        @Override
        public Reader getCharacterStream() {
            return this.characterStream;
        }

        @Override
        public void setCharacterStream(final Reader characterStream) {
            this.characterStream = characterStream;
        }

        @Override
        public String getStringData() {
            return this.stringData;
        }

        @Override
        public void setStringData(final String stringData) {
            this.stringData = stringData;
        }

        @Override
        public String getBaseURI() {
            return this.baseURI;
        }

        @Override
        public void setBaseURI(final String baseURI) {
            this.baseURI = baseURI;
        }

        @Override
        public String getEncoding() {
            return this.encoding;
        }

        @Override
        public void setEncoding(final String encoding) {
            this.encoding = encoding;
        }

        @Override
        public boolean getCertifiedText() {
            return this.certifiedText;
        }

        @Override
        public void setCertifiedText(final boolean certifiedText) {
            this.certifiedText = certifiedText;
        }

        /**
         * A identidade do recurso e definida na construcao. Falhar alto e melhor do que aceitar
         * em silencio uma escrita que nao teria efeito sobre o conteudo ja aberto.
         */
        @Override
        public void setSystemId(final String systemId) {
            throw new UnsupportedOperationException("systemId e imutavel");
        }

        @Override
        public void setPublicId(final String publicId) {
            throw new UnsupportedOperationException("publicId e imutavel");
        }
    }
}
