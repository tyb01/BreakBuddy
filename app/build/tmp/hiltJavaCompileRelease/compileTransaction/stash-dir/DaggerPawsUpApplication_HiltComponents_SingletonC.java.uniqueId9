package com.pawsup.application;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.pawsup.apppicker.AppPickerActivity;
import com.pawsup.apppicker.AppPickerViewModel;
import com.pawsup.apppicker.AppPickerViewModel_HiltModules;
import com.pawsup.apppicker.InstalledAppsRepository;
import com.pawsup.billing.BillingClientWrapper;
import com.pawsup.billing.BillingRepository;
import com.pawsup.break_experience.BreakOverlayActivity;
import com.pawsup.break_experience.BreakOverlayActivity_MembersInjector;
import com.pawsup.break_experience.BreakOverlayViewModel;
import com.pawsup.break_experience.BreakOverlayViewModel_HiltModules;
import com.pawsup.cats.CatRegistry;
import com.pawsup.data.UserPreferences;
import com.pawsup.di.AppModule_ProvideCatRegistryFactory;
import com.pawsup.di.AppModule_ProvideUserPreferencesFactory;
import com.pawsup.di.PackageManagerModule_ProvidePackageManagerFactory;
import com.pawsup.monitoring.BootReceiver;
import com.pawsup.monitoring.BootReceiver_MembersInjector;
import com.pawsup.monitoring.KeepAliveReceiver;
import com.pawsup.monitoring.KeepAliveReceiver_MembersInjector;
import com.pawsup.monitoring.MonitoringRepository;
import com.pawsup.monitoring.MonitoringService;
import com.pawsup.monitoring.MonitoringService_MembersInjector;
import com.pawsup.monitoring.WatchdogWorker;
import com.pawsup.monitoring.WatchdogWorker_AssistedFactory;
import com.pawsup.onboarding.OnboardingActivity;
import com.pawsup.onboarding.OnboardingActivity_MembersInjector;
import com.pawsup.paywall.PaywallActivity;
import com.pawsup.paywall.PaywallViewModel;
import com.pawsup.paywall.PaywallViewModel_HiltModules;
import com.pawsup.settings.SettingsActivity;
import com.pawsup.settings.SettingsActivity_MembersInjector;
import com.pawsup.settings.SettingsViewModel;
import com.pawsup.settings.SettingsViewModel_HiltModules;
import com.pawsup.ui.MainActivity;
import com.pawsup.ui.MainActivity_MembersInjector;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DaggerPawsUpApplication_HiltComponents_SingletonC {
  private DaggerPawsUpApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public PawsUpApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements PawsUpApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public PawsUpApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements PawsUpApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public PawsUpApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements PawsUpApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public PawsUpApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements PawsUpApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public PawsUpApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements PawsUpApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public PawsUpApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements PawsUpApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public PawsUpApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements PawsUpApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public PawsUpApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends PawsUpApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends PawsUpApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends PawsUpApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends PawsUpApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectAppPickerActivity(AppPickerActivity arg0) {
    }

    @Override
    public void injectBreakOverlayActivity(BreakOverlayActivity arg0) {
      injectBreakOverlayActivity2(arg0);
    }

    @Override
    public void injectOnboardingActivity(OnboardingActivity arg0) {
      injectOnboardingActivity2(arg0);
    }

    @Override
    public void injectPaywallActivity(PaywallActivity arg0) {
    }

    @Override
    public void injectSettingsActivity(SettingsActivity arg0) {
      injectSettingsActivity2(arg0);
    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
      injectMainActivity2(arg0);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>of(LazyClassKeyProvider.com_pawsup_apppicker_AppPickerViewModel, AppPickerViewModel_HiltModules.KeyModule.provide(), LazyClassKeyProvider.com_pawsup_break_experience_BreakOverlayViewModel, BreakOverlayViewModel_HiltModules.KeyModule.provide(), LazyClassKeyProvider.com_pawsup_paywall_PaywallViewModel, PaywallViewModel_HiltModules.KeyModule.provide(), LazyClassKeyProvider.com_pawsup_settings_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()));
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private BreakOverlayActivity injectBreakOverlayActivity2(BreakOverlayActivity instance) {
      BreakOverlayActivity_MembersInjector.injectCatRegistry(instance, singletonCImpl.provideCatRegistryProvider.get());
      return instance;
    }

    private OnboardingActivity injectOnboardingActivity2(OnboardingActivity instance) {
      OnboardingActivity_MembersInjector.injectPrefs(instance, singletonCImpl.provideUserPreferencesProvider.get());
      OnboardingActivity_MembersInjector.injectCatRegistry(instance, singletonCImpl.provideCatRegistryProvider.get());
      return instance;
    }

    private SettingsActivity injectSettingsActivity2(SettingsActivity instance) {
      SettingsActivity_MembersInjector.injectCatRegistry(instance, singletonCImpl.provideCatRegistryProvider.get());
      return instance;
    }

    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectPrefs(instance, singletonCImpl.provideUserPreferencesProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_pawsup_break_experience_BreakOverlayViewModel = "com.pawsup.break_experience.BreakOverlayViewModel";

      static String com_pawsup_apppicker_AppPickerViewModel = "com.pawsup.apppicker.AppPickerViewModel";

      static String com_pawsup_settings_SettingsViewModel = "com.pawsup.settings.SettingsViewModel";

      static String com_pawsup_paywall_PaywallViewModel = "com.pawsup.paywall.PaywallViewModel";

      @KeepFieldType
      BreakOverlayViewModel com_pawsup_break_experience_BreakOverlayViewModel2;

      @KeepFieldType
      AppPickerViewModel com_pawsup_apppicker_AppPickerViewModel2;

      @KeepFieldType
      SettingsViewModel com_pawsup_settings_SettingsViewModel2;

      @KeepFieldType
      PaywallViewModel com_pawsup_paywall_PaywallViewModel2;
    }
  }

  private static final class ViewModelCImpl extends PawsUpApplication_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AppPickerViewModel> appPickerViewModelProvider;

    private Provider<BreakOverlayViewModel> breakOverlayViewModelProvider;

    private Provider<PaywallViewModel> paywallViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.appPickerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.breakOverlayViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.paywallViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>of(LazyClassKeyProvider.com_pawsup_apppicker_AppPickerViewModel, ((Provider) appPickerViewModelProvider), LazyClassKeyProvider.com_pawsup_break_experience_BreakOverlayViewModel, ((Provider) breakOverlayViewModelProvider), LazyClassKeyProvider.com_pawsup_paywall_PaywallViewModel, ((Provider) paywallViewModelProvider), LazyClassKeyProvider.com_pawsup_settings_SettingsViewModel, ((Provider) settingsViewModelProvider)));
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_pawsup_apppicker_AppPickerViewModel = "com.pawsup.apppicker.AppPickerViewModel";

      static String com_pawsup_settings_SettingsViewModel = "com.pawsup.settings.SettingsViewModel";

      static String com_pawsup_break_experience_BreakOverlayViewModel = "com.pawsup.break_experience.BreakOverlayViewModel";

      static String com_pawsup_paywall_PaywallViewModel = "com.pawsup.paywall.PaywallViewModel";

      @KeepFieldType
      AppPickerViewModel com_pawsup_apppicker_AppPickerViewModel2;

      @KeepFieldType
      SettingsViewModel com_pawsup_settings_SettingsViewModel2;

      @KeepFieldType
      BreakOverlayViewModel com_pawsup_break_experience_BreakOverlayViewModel2;

      @KeepFieldType
      PaywallViewModel com_pawsup_paywall_PaywallViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.pawsup.apppicker.AppPickerViewModel 
          return (T) new AppPickerViewModel(singletonCImpl.installedAppsRepositoryProvider.get(), singletonCImpl.provideUserPreferencesProvider.get());

          case 1: // com.pawsup.break_experience.BreakOverlayViewModel 
          return (T) new BreakOverlayViewModel(singletonCImpl.provideUserPreferencesProvider.get(), singletonCImpl.provideCatRegistryProvider.get(), viewModelCImpl.savedStateHandle);

          case 2: // com.pawsup.paywall.PaywallViewModel 
          return (T) new PaywallViewModel(singletonCImpl.provideCatRegistryProvider.get(), singletonCImpl.billingClientWrapperProvider.get(), singletonCImpl.billingRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 3: // com.pawsup.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.provideUserPreferencesProvider.get(), singletonCImpl.provideCatRegistryProvider.get(), singletonCImpl.billingRepositoryProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends PawsUpApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends PawsUpApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectMonitoringService(MonitoringService arg0) {
      injectMonitoringService2(arg0);
    }

    private MonitoringService injectMonitoringService2(MonitoringService instance) {
      MonitoringService_MembersInjector.injectPrefs(instance, singletonCImpl.provideUserPreferencesProvider.get());
      MonitoringService_MembersInjector.injectRepo(instance, singletonCImpl.monitoringRepositoryProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends PawsUpApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<MonitoringRepository> monitoringRepositoryProvider;

    private Provider<UserPreferences> provideUserPreferencesProvider;

    private Provider<WatchdogWorker_AssistedFactory> watchdogWorker_AssistedFactoryProvider;

    private Provider<BillingClientWrapper> billingClientWrapperProvider;

    private Provider<CatRegistry> provideCatRegistryProvider;

    private Provider<PackageManager> providePackageManagerProvider;

    private Provider<InstalledAppsRepository> installedAppsRepositoryProvider;

    private Provider<BillingRepository> billingRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return ImmutableMap.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>of("com.pawsup.monitoring.WatchdogWorker", ((Provider) watchdogWorker_AssistedFactoryProvider));
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.monitoringRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<MonitoringRepository>(singletonCImpl, 1));
      this.provideUserPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<UserPreferences>(singletonCImpl, 2));
      this.watchdogWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<WatchdogWorker_AssistedFactory>(singletonCImpl, 0));
      this.billingClientWrapperProvider = DoubleCheck.provider(new SwitchingProvider<BillingClientWrapper>(singletonCImpl, 3));
      this.provideCatRegistryProvider = DoubleCheck.provider(new SwitchingProvider<CatRegistry>(singletonCImpl, 4));
      this.providePackageManagerProvider = DoubleCheck.provider(new SwitchingProvider<PackageManager>(singletonCImpl, 6));
      this.installedAppsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<InstalledAppsRepository>(singletonCImpl, 5));
      this.billingRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<BillingRepository>(singletonCImpl, 7));
    }

    @Override
    public void injectPawsUpApplication(PawsUpApplication pawsUpApplication) {
      injectPawsUpApplication2(pawsUpApplication);
    }

    @Override
    public void injectBootReceiver(BootReceiver arg0) {
      injectBootReceiver2(arg0);
    }

    @Override
    public void injectKeepAliveReceiver(KeepAliveReceiver arg0) {
      injectKeepAliveReceiver2(arg0);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private PawsUpApplication injectPawsUpApplication2(PawsUpApplication instance) {
      PawsUpApplication_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      PawsUpApplication_MembersInjector.injectBillingClient(instance, billingClientWrapperProvider.get());
      PawsUpApplication_MembersInjector.injectCatRegistry(instance, provideCatRegistryProvider.get());
      return instance;
    }

    private BootReceiver injectBootReceiver2(BootReceiver instance) {
      BootReceiver_MembersInjector.injectPrefs(instance, provideUserPreferencesProvider.get());
      return instance;
    }

    private KeepAliveReceiver injectKeepAliveReceiver2(KeepAliveReceiver instance) {
      KeepAliveReceiver_MembersInjector.injectPrefs(instance, provideUserPreferencesProvider.get());
      KeepAliveReceiver_MembersInjector.injectRepo(instance, monitoringRepositoryProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.pawsup.monitoring.WatchdogWorker_AssistedFactory 
          return (T) new WatchdogWorker_AssistedFactory() {
            @Override
            public WatchdogWorker create(Context context, WorkerParameters params) {
              return new WatchdogWorker(context, params, singletonCImpl.monitoringRepositoryProvider.get(), singletonCImpl.provideUserPreferencesProvider.get());
            }
          };

          case 1: // com.pawsup.monitoring.MonitoringRepository 
          return (T) new MonitoringRepository();

          case 2: // com.pawsup.data.UserPreferences 
          return (T) AppModule_ProvideUserPreferencesFactory.provideUserPreferences(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.pawsup.billing.BillingClientWrapper 
          return (T) new BillingClientWrapper(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.pawsup.cats.CatRegistry 
          return (T) AppModule_ProvideCatRegistryFactory.provideCatRegistry();

          case 5: // com.pawsup.apppicker.InstalledAppsRepository 
          return (T) new InstalledAppsRepository(singletonCImpl.providePackageManagerProvider.get());

          case 6: // android.content.pm.PackageManager 
          return (T) PackageManagerModule_ProvidePackageManagerFactory.providePackageManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // com.pawsup.billing.BillingRepository 
          return (T) new BillingRepository(singletonCImpl.billingClientWrapperProvider.get(), singletonCImpl.provideUserPreferencesProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
