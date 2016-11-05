/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.github.jknack.antlr4ide.ui.preferences;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.xtext.Constants;
import org.eclipse.xtext.builder.DerivedResourceCleanerJob;
import org.eclipse.xtext.builder.EclipseOutputConfigurationProvider;
import org.eclipse.xtext.ui.IImageHelper;
import org.eclipse.xtext.ui.editor.preferences.PreferenceStoreAccessImpl;
import org.eclipse.xtext.ui.preferences.OptionsConfigurationBlock;
import org.eclipse.xtext.ui.preferences.PropertyAndPreferencePage;

import com.google.common.collect.MapDifference.ValueDifference;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * @author Michael Clay - Initial contribution and API
 * @since 2.1
 */
@SuppressWarnings("restriction")
public class BuilderPreferencePage extends PropertyAndPreferencePage {
  private static final boolean DEBUG = false; 
  private OptionsConfigurationBlock builderConfigurationBlock;

  private EclipseOutputConfigurationProvider configurationProvider;

  private String languageName;

  private PreferenceStoreAccessImpl preferenceStoreAccessImpl;

  private Provider<DerivedResourceCleanerJob> cleanerProvider;

  private IImageHelper imageHelper;

  @Inject
  public void setCleanerProvider(final Provider<DerivedResourceCleanerJob> cleanerProvider) {
	  if(DEBUG) System.out.println("BuilderPreferencePage setCleanerProvider ");

    this.cleanerProvider = cleanerProvider;
  }

  @Inject
  public void setLanguageName(@Named(Constants.LANGUAGE_NAME) final String languageName) {
	  if(DEBUG) System.out.println("BuilderPreferencePage setLanguageName languageName>"+languageName+"<");
    this.languageName = languageName;
  }

  @Inject
  public void setConfigurationProvider(
      final EclipseOutputConfigurationProvider configurationProvider) {
	  if(DEBUG) System.out.println("BuilderPreferencePage setConfigurationProvider ");
    this.configurationProvider = configurationProvider;
  }

  @Inject
  public void setPreferenceStoreAccessImpl(final PreferenceStoreAccessImpl preferenceStoreAccessImpl) {
	  if(DEBUG) System.out.println("BuilderPreferencePage setPreferenceStoreAccessImpl ");
    this.preferenceStoreAccessImpl = preferenceStoreAccessImpl;
  }

  @Inject
  public void setImageHelper(final IImageHelper imageHelper) {
	  if(DEBUG) System.out.println("BuilderPreferencePage setImageHelper ");
    this.imageHelper = imageHelper;
  }

  @Override
  public void createControl(final Composite parent) {
	  if(DEBUG) System.out.println("BuilderPreferencePage createControl ");
    IWorkbenchPreferenceContainer container = (IWorkbenchPreferenceContainer) getContainer();
    IPreferenceStore preferenceStore = preferenceStoreAccessImpl
        .getWritablePreferenceStore(getProject());
    builderConfigurationBlock = new BuilderConfigurationBlock(getProject(), preferenceStore,
        configurationProvider, container, imageHelper);
    builderConfigurationBlock.setStatusChangeListener(getNewStatusChangedListener());
    
    super.createControl(parent);
  }

  @Override
  protected Control createPreferenceContent(final Composite composite,
      final IPreferencePageContainer preferencePageContainer) {
	  if(DEBUG) System.out.println("BuilderPreferencePage createPreferenceContent ");
	  
    return builderConfigurationBlock.createContents(composite);
  }

  @Override
  protected boolean hasProjectSpecificOptions(final IProject project) {
	  if(DEBUG) System.out.println("BuilderPreferencePage hasProjectSpecificOptions project>"+project.toString()+"<");
	  if(DEBUG) System.out.println("BuilderPreferencePage          builderConfigurationBlock==null "+(builderConfigurationBlock==null));
	  try {
    return builderConfigurationBlock.hasProjectSpecificOptions(project);
	  }
	  catch (Exception ex) {
		  System.out.println("!!!BuilderPreferencePage hasProjectSpecificOptions Exception "+ex);
		  ex.printStackTrace();
		  return false;
	  }
  }

  @Override
  protected String getPreferencePageID() {
	  if(DEBUG) System.out.println("BuilderPreferencePage getPreferencePageID >"+languageName + ".compiler.preferencePage"+"<");
	  
    return languageName + ".compiler.preferencePage";
  }

  @Override
  protected String getPropertyPageID() {
	  if(DEBUG) System.out.println("BuilderPreferencePage getPropertyPageID >"+languageName + ".compiler.propertyPage"+"<");
    return languageName + ".compiler.propertyPage";
  }

  @Override
  public void dispose() {
	  if(DEBUG) System.out.println("BuilderPreferencePage dispose builderConfigurationBlock != null >"+(builderConfigurationBlock != null)+"<");
	  
    if (builderConfigurationBlock != null) {
      builderConfigurationBlock.dispose();
    }
    super.dispose();
  }

  @Override
  protected void enableProjectSpecificSettings(final boolean useProjectSpecificSettings) {
	  if(DEBUG) System.out.println("BuilderPreferencePage enableProjectSpecificSettings useProjectSpecificSettings>"+(useProjectSpecificSettings)+"<");
	  
    super.enableProjectSpecificSettings(useProjectSpecificSettings);
    if (builderConfigurationBlock != null) {
      builderConfigurationBlock.useProjectSpecificSettings(useProjectSpecificSettings);
    }
  }

  @Override
  protected void performDefaults() {
	  if(DEBUG) System.out.println("BuilderPreferencePage performDefaults");
	  
    super.performDefaults();
    if (builderConfigurationBlock != null) {
      builderConfigurationBlock.performDefaults();
    }
  }

  @Override
  public boolean performOk() {
	  if(DEBUG) System.out.println("BuilderPreferencePage performOk");
	  
    if (builderConfigurationBlock != null) {
      scheduleCleanerJobIfNecessary(getContainer());
      if (!builderConfigurationBlock.performOk()) {
        return false;
      }
    }
    return super.performOk();
  }

  @Override
  public void performApply() {
	  if(DEBUG) System.out.println("BuilderPreferencePage performApply");
	  
    if (builderConfigurationBlock != null) {
      scheduleCleanerJobIfNecessary(null);
      builderConfigurationBlock.performApply();
    }
  }

  @Override
  public void setElement(final IAdaptable element) {
	  if(DEBUG) System.out.println("BuilderPreferencePage setElement");
	  
    super.setElement(element);
    setDescription(null); // no description for property page
  }

  private void scheduleCleanerJobIfNecessary(final IPreferencePageContainer preferencePageContainer) {
	  if(DEBUG) System.out.println("BuilderPreferencePage scheduleCleanerJobIfNecessary");
	  
    Map<String, ValueDifference<String>> changes = builderConfigurationBlock.getPreferenceChanges();
    for (String key : changes.keySet()) {
      if (key.matches("^" + EclipseOutputConfigurationProvider.OUTPUT_PREFERENCE_TAG + "\\.\\w+\\."
          + EclipseOutputConfigurationProvider.OUTPUT_DIRECTORY + "$")) {
        ValueDifference<String> difference = changes.get(key);
        scheduleCleanerJob(preferencePageContainer, difference.rightValue());
      }
    }
  }

  private void scheduleCleanerJob(final IPreferencePageContainer preferencePageContainer,
      final String folderNameToClean) {
	  if(DEBUG) System.out.println("BuilderPreferencePage scheduleCleanerJob");
	  
    DerivedResourceCleanerJob derivedResourceCleanerJob = cleanerProvider.get();
    derivedResourceCleanerJob.setUser(true);
    derivedResourceCleanerJob.initialize(getProject(), folderNameToClean);
    if (preferencePageContainer != null) {
      IWorkbenchPreferenceContainer container = (IWorkbenchPreferenceContainer) getContainer();
      container.registerUpdateJob(derivedResourceCleanerJob);
    } else {
      derivedResourceCleanerJob.schedule();
    }
  }

}
