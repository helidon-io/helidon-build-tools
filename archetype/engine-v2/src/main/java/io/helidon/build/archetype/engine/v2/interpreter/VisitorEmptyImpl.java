package io.helidon.build.archetype.engine.v2.interpreter;

import io.helidon.build.archetype.engine.v2.descriptor.FileSet;
import io.helidon.build.archetype.engine.v2.descriptor.FileSets;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;
import io.helidon.build.archetype.engine.v2.descriptor.Template;
import io.helidon.build.archetype.engine.v2.descriptor.Templates;
import io.helidon.build.archetype.engine.v2.descriptor.Transformation;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

abstract class VisitorEmptyImpl<T> implements Visitor<T> {
    @Override
    public void visit(Visitable v, T arg) {

    }

    @Override
    public void visit(Flow v, T arg) {

    }

    @Override
    public void visit(XmlDescriptor v, T arg) {

    }

    @Override
    public void visit(StepAST step, T arg) {

    }

    @Override
    public void visit(InputAST input, T arg) {

    }

    @Override
    public void visit(InputBooleanAST input, T arg) {

    }

    @Override
    public void visit(InputEnumAST input, T arg) {

    }

    @Override
    public void visit(InputListAST input, T arg) {

    }

    @Override
    public void visit(InputTextAST input, T arg) {

    }

    @Override
    public void visit(ExecAST exec, T arg) {

    }

    @Override
    public void visit(SourceAST source, T arg) {

    }

    @Override
    public void visit(ContextAST context, T arg) {

    }

    @Override
    public void visit(ContextBooleanAST contextBoolean, T arg) {

    }

    @Override
    public void visit(ContextEnumAST contextEnum, T arg) {

    }

    @Override
    public void visit(ContextListAST contextList, T arg) {

    }

    @Override
    public void visit(ContextTextAST contextText, T arg) {

    }

    @Override
    public void visit(OptionAST option, T arg) {

    }

    @Override
    public void visit(OutputAST output, T arg) {

    }

    @Override
    public void visit(TransformationAST transformation, T arg) {

    }

    @Override
    public void visit(FileSetsAST fileSets, T arg) {

    }

    @Override
    public void visit(FileSetAST fileSet, T arg) {

    }

    @Override
    public void visit(TemplateAST template, T arg) {

    }

    @Override
    public void visit(TemplatesAST templates, T arg) {

    }

    @Override
    public void visit(ModelAST model, T arg) {

    }

    @Override
    public void visit(IfStatement statement, T arg) {

    }

    @Override
    public void visit(ModelKeyValueAST value, T arg) {

    }

    @Override
    public void visit(ValueTypeAST value, T arg) {

    }

    @Override
    public void visit(ModelKeyListAST list, T arg) {

    }

    @Override
    public void visit(MapTypeAST map, T arg) {

    }

    @Override
    public void visit(ListTypeAST list, T arg) {

    }

    @Override
    public void visit(ModelKeyMapAST map, T arg) {

    }
}
