/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.resources;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.fs.nio.NIOFile;
import org.jkiss.dbeaver.model.fs.nio.NIOFileStore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNNodeWithResource;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ProgramInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Default resource handler
 */
public class DefaultResourceHandlerImpl extends AbstractResourceHandler {

    public static final DefaultResourceHandlerImpl INSTANCE = new DefaultResourceHandlerImpl();

    @Override
    public int getFeatures(IResource resource) {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        } else if (resource instanceof IFolder) {
            return FEATURE_DELETE | FEATURE_RENAME | FEATURE_CREATE_FOLDER | FEATURE_MOVE_INTO;
        }
        return super.getFeatures(resource);
    }

    @NotNull
    @Override
    public String getTypeName(@NotNull IResource resource) {
        final ProgramInfo program = ProgramInfo.getProgram(resource);
        if (program != null) {
            return program.getProgram().getName();
        }
        return "resource"; //$NON-NLS-1$
    }

    @Override
    public String getResourceDescription(@NotNull IResource resource) {
        return "";
    }

    @NotNull
    @Override
    public DBNResource makeNavigatorNode(@NotNull DBNNode parentNode, @NotNull IResource resource) throws CoreException, DBException {
        DBNResource node = super.makeNavigatorNode(parentNode, resource);
        updateNavigatorNodeFromResource(node, resource);
        return node;
    }

    @Override
    public void updateNavigatorNodeFromResource(@NotNull DBNNodeWithResource node, @NotNull IResource resource) {
        super.updateNavigatorNodeFromResource(node, resource);
        String fileExtension = resource.getFileExtension();
        if (!CommonUtils.isEmpty(fileExtension)) {
            setNodeIconFromFileType(node, fileExtension);
        }
    }

    public void setNodeIconFromFileType(@NotNull DBNNodeWithResource node, @NotNull String fileExt) {
        ProgramInfo program = ProgramInfo.getProgram(fileExt);
        if (program != null && program.getImage() != null) {
            node.setResourceImage(program.getImage());
        }
    }

    @Override
    public void openResource(@NotNull IResource resource) throws CoreException, DBException {
        if (resource instanceof NIOFile) {
            NIOFileStore fileStore = new NIOFileStore(resource.getLocationURI(), ((NIOFile) resource).getNioPath());
            FileStoreEditorInput editorInput = new FileStoreEditorInput(fileStore);

            // open the editor on the file
            IEditorDescriptor editorDesc;
            try {
                editorDesc = IDE.getEditorDescriptor((IFile)resource, true, true);
            } catch (OperationCanceledException ex) {
                return;
            }

            IDE.openEditor(
                UIUtils.getActiveWorkbenchWindow().getActivePage(),
                editorInput,
                editorDesc.getId());
        } else if (resource instanceof IFile) {
            IDE.openEditor(UIUtils.getActiveWorkbenchWindow().getActivePage(), (IFile) resource);
        } else if (resource instanceof IFolder) {
            DBWorkbench.getPlatformUI().executeShellProgram(resource.getLocation().toOSString());
        }
    }

}
