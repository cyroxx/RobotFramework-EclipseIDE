/**
 * Copyright 2012-2014 Nitor Creations Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nitorcreations.robotframework.eclipseide.editors;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.nitorcreations.robotframework.eclipseide.Activator;
import com.nitorcreations.robotframework.eclipseide.PluginContext;
import com.nitorcreations.robotframework.eclipseide.builder.parser.RobotFile;
import com.nitorcreations.robotframework.eclipseide.editors.outline.ParsedStringEntry;
import com.nitorcreations.robotframework.eclipseide.editors.outline.RobotOutlinePage;

/**
 * https://robotframework.googlecode.com/hg/doc/userguide/ RobotFrameworkUserGuide.html?r=2.6.1 http:/
 * /help.eclipse.org/helios/index.jsp ?topic=/org.eclipse.platform.doc.isv/reference/api/org/ eclipse
 * /jface/text/source/package-summary.html
 * 
 * @author xkr47
 */
public class RobotFrameworkTextfileEditor extends TextEditor {

    public static final String EDITOR_ID = RobotFrameworkTextfileEditor.class.getName();

    private ColorManager colorManager;

    private RobotOutlinePage outlinePage;

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        handleClosePossiblyOpenDocument();
        super.doSetInput(input);
        if (input != null) {
            handleOpenDocument();
        }
    }

    @Override
    public void dispose() {
        handleClosePossiblyOpenDocument();
        colorManager.dispose();
        super.dispose();
    }

    private void handleClosePossiblyOpenDocument() {
        IEditorInput old = getEditorInput();
        if (old != null) {
            handleCloseDocument(old);
        }
    }

    private void handleOpenDocument() throws CoreException {
        IDocument document = getEditedDocument();
        System.out.println("Opened document " + getEditorInput() + " -> " + document);
        ensureRFSupportedLineEndings(document);
        PluginContext.getResourceManager().registerEditor(this);
    }

    private static final Pattern BAD_LINE_ENDING_PATTERN = Pattern.compile("\r(?:[^\n]|$)");

    private void ensureRFSupportedLineEndings(IDocument document) throws CoreException {
        String doc = document.get();
        if (BAD_LINE_ENDING_PATTERN.matcher(doc).find()) {
            /*
             * The Robot Framework does not support CR-only line endings, so it's informative to show an error in the
             * editor rather than pretend everything is ok.
             */
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "File contains MAC OS 9 line endings¹ which are not supported by Robot Framework.\n\nPlease convert it using some appropriate tool.\n\n¹) a Carriage Return character not immediately followed by a Line Feed character"));
        }
    }

    private void handleCloseDocument(IEditorInput old) {
        System.out.println("Closing document " + old);
        PluginContext.getResourceManager().unregisterEditor(this);
        RobotFile.erase(getEditedDocument());
    }

    public IDocument getEditedDocument() {
        return getDocumentProvider().getDocument(getEditorInput());
    }

    public IFile getEditedFile() {
        return getEditorInput().getAdapter(IFile.class);
    }

    @Override
    protected void initializeViewerColors(ISourceViewer viewer) {
        super.initializeViewerColors(viewer);
        /*
         * Workaround for issue mentioned in #36. Steps to repeat: 1. Comment out the line below 2. Change some robot
         * ide syntax color preference and apply the changes 3. change the default text editor foreground in
         * "General/Editors/Text Editors". As a result the editor colors are wrongly rendered. Making any change to the
         * file fixes the problem. The line below seems to work around this problem.
         */
        viewer.invalidateTextPresentation();
    }

    @Override
    protected boolean affectsTextPresentation(PropertyChangeEvent event) {
        return colorManager.isColorPreference(event.getProperty());
    }

    @Override
    protected void initializeEditor() {
        super.initializeEditor();
        /*
         * Extend the base preferences of the editor (this class) with our own plugin preferences. The base editor
         * (superclass) listens to changes in the preference store and uses affectsTextPresentation() above to determine
         * whether to redraw the editor.
         * 
         * [ASSUMPTION] We need to include/extend the preferences of the base editor in order for base editor to operate
         * accoding to generic editor preferences chosen by the user (font, background etc).
         */
        IPreferenceStore baseEditorPreferenceStore = getPreferenceStore();
        IPreferenceStore ourPreferenceStore = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(new ChainedPreferenceStore(new IPreferenceStore[] { ourPreferenceStore, baseEditorPreferenceStore }));

        colorManager = new ColorManager();

        setSourceViewerConfiguration(new RobotSourceViewerConfiguration(colorManager, getPreferenceStore()));
        setDocumentProvider(new FileDocumentProvider());
    }

    @Override
    public Object getAdapter(Class required) {
        if (IContentOutlinePage.class.equals(required)) {
            if (outlinePage == null) {
                outlinePage = new RobotOutlinePage(getDocumentProvider());
                outlinePage.addSelectionChangedListener(new RobotOutlineSelectionChangedListener(this));
                if (getEditorInput() != null)
                    outlinePage.setInput(getEditorInput());
            }
            return outlinePage;
        }
        return super.getAdapter(required);
    }

    private final class RobotOutlineSelectionChangedListener implements ISelectionChangedListener {
        private final ITextEditor textEditor;

        public RobotOutlineSelectionChangedListener(ITextEditor textEditor) {
            this.textEditor = textEditor;
        }

        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            if (event.getSelection() instanceof IStructuredSelection) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();

                // so far, we only support the selection of one entry at a time - see RobotOutlinePage#getTreeStyle()
                Object domain = selection.getFirstElement();
                if (domain instanceof ParsedStringEntry) {
                    ParsedStringEntry lineEntry = (ParsedStringEntry) domain;
                    int startCharPos = lineEntry.getStartCharPos();
                    int endCharPos = lineEntry.getEndCharPos();
                    textEditor.selectAndReveal(startCharPos, endCharPos - startCharPos);
                }
            }
        }
    }
}

// 190312 1720 xxxx
