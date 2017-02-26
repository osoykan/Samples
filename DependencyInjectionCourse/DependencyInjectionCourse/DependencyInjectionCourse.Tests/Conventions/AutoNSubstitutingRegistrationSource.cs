using System;
using System.Collections.Generic;
using System.Linq;

using Autofac.Builder;
using Autofac.Core;

using NSubstitute.Core;

namespace DependencyInjectionCourse.Tests.Conventions
{
    public class AutoNSubstitutingRegistrationSource : IRegistrationSource
    {
        private readonly ISubstituteFactory _substituteFactory;

        public AutoNSubstitutingRegistrationSource()
        {
            _substituteFactory = SubstitutionContext.Current.SubstituteFactory;
        }

        public IEnumerable<IComponentRegistration> RegistrationsFor(Service service, Func<Service, IEnumerable<IComponentRegistration>> registrationAccessor)
        {
            var swt = service as IServiceWithType;
            if (swt == null)
            {
                yield break;
            }

            IEnumerable<IComponentRegistration> existingReg = registrationAccessor(service);
            if (existingReg.Any())
            {
                yield break;
            }

            IComponentRegistration reg = RegistrationBuilder.ForDelegate((c, p) =>
            {
                object fakeInstance = _substituteFactory.Create(new Type[] { swt.ServiceType }, null);
                return fakeInstance;
            }).As(swt.ServiceType)
            .SingleInstance()
            .CreateRegistration();

            yield return reg;
        }

        public bool IsAdapterForIndividualComponents { get; }
    }
}
