package com.googlecode.barongreenback.html;

import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Callable2;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Sequences;
import com.googlecode.totallylazy.records.xml.Xml;
import com.googlecode.utterlyidle.Request;
import com.googlecode.utterlyidle.RequestBuilder;
import org.w3c.dom.Node;

public class Form {
    private final Node form;

    public Form(Node form) {
        this.form = form;
    }

    public Request submit(String submitXpath) {
        String action = Xml.selectContents(form, "@action");
        String method = Xml.selectContents(form, "@method");
        Sequence<NameValue> inputs = nameValuePairs("//input[not(@type='submit')]|//select|" + submitXpath );
        return inputs.fold(new RequestBuilder(method, action), addFormParams()).build();
    }

    private Sequence<NameValue> nameValuePairs(String xpath) {
        return Xml.selectNodes(form, xpath).flatMap(toNameAndValue());
    }

    private Callable1<? super Node, Sequence<NameValue>> toNameAndValue() {
        return new Callable1<Node, Sequence<NameValue>>() {
            public Sequence<NameValue> call(Node node) throws Exception {
                String type = type(node);
                if (type.equals("select")) {
                    return Sequences.<NameValue>sequence(new Select(node));
                }
                if (type.equals("checkbox")) {
                    Checkbox checkbox = new Checkbox(node);
                    if (checkbox.checked()) {
                        return Sequences.<NameValue>sequence(checkbox);
                    }
                    return Sequences.empty();
                }
                return Sequences.<NameValue>sequence(new Input(node));
            }
        };
    }

    private String type(Node node) {
        String nodeName = node.getNodeName();
        if (nodeName.equals("input")) {
            return Xml.selectContents(node, "@type");
        }
        return nodeName;
    }

    private Callable2<RequestBuilder, NameValue, RequestBuilder> addFormParams() {
        return new Callable2<RequestBuilder, NameValue, RequestBuilder>() {
            public RequestBuilder call(RequestBuilder requestBuilder, NameValue nameValue) throws Exception {
                return requestBuilder.withForm(nameValue.name(), nameValue.value());
            }
        };
    }

}
