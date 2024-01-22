using System;

using Castle.MicroKernel.Registration;
using Castle.Windsor;

namespace QuartzCore.Dependency
{
    public class IocManager
    {
        static IocManager()
        {
            Instance = new IocManager();
        }

        public IocManager()
        {
            Container = new WindsorContainer();

            Container.Register(
                Component.For<IocManager>().UsingFactoryMethod(() => this)
                );
        }

        public IWindsorContainer Container { get; }

        public static IocManager Instance { get; private set; }

        public void Release(object objectToRelase)
        {
            Container.Release(objectToRelase);
        }

        public T Resolve<T>()
        {
            return Container.Resolve<T>();
        }

        public T Resolve<T>(Type type)
        {
            return (T)Container.Resolve(type);
        }

        public object Resolve(Type type)
        {
            return Container.Resolve(type);
        }

        public object Resolve(Type type, object argumentsAsAnonymousType)
        {
            return Container.Resolve(type, argumentsAsAnonymousType);
        }

        public void Register<TType>(LifeStyle lifeStyle = LifeStyle.Singleton) where TType : class
        {
            Container.Register(ApplyLifestyle(Component.For<TType>(), lifeStyle));
        }

        public void Register(Type type, LifeStyle lifeStyle = LifeStyle.Singleton)
        {
            Container.Register(ApplyLifestyle(Component.For(type), lifeStyle));
        }

        public void Register<TType, TImpl>(LifeStyle lifeStyle = LifeStyle.Singleton)
            where TType : class
            where TImpl : class, TType
        {
            Container.Register(ApplyLifestyle(Component.For<TType, TImpl>().ImplementedBy<TImpl>(), lifeStyle));
        }

        public void Register(Type type, Type impl, LifeStyle lifeStyle = LifeStyle.Singleton)
        {
            Container.Register(ApplyLifestyle(Component.For(type, impl).ImplementedBy(impl), lifeStyle));
        }

        public void RegisterUsingFactoryMethod<TService>(Func<TService> implementation, LifeStyle lifeStyle = LifeStyle.Singleton) where TService : class
        {
            Container.Register(ApplyLifestyle(Component.For<TService>().UsingFactoryMethod(implementation), lifeStyle));
        }

        public bool IsRegistered<TType>()
        {
            return Container.Kernel.HasComponent(typeof(TType));
        }

        private static ComponentRegistration<TType> ApplyLifestyle<TType>(ComponentRegistration<TType> registration, LifeStyle lifeStyle)
            where TType : class
        {
            switch (lifeStyle)
            {
                case LifeStyle.Transient:
                    return registration.LifestyleTransient();
                case LifeStyle.Singleton:
                    return registration.LifestyleSingleton();
                default:
                    return registration;
            }
        }
    }
}