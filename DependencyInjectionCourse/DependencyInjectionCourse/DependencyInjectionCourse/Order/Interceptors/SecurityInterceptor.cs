using System;
using System.Threading;

using Castle.DynamicProxy;

using DependencyInjectionCourse.Logger;

namespace DependencyInjectionCourse.Interceptors
{
    public class SecurityInterceptor : IInterceptor
    {
        public SecurityInterceptor()
        {
            Logger = NullLogger.Instance;
        }

        public ILogger Logger { get; set; }

        public void Intercept(IInvocation invocation)
        {
            bool isGranted = Thread.CurrentPrincipal.IsInRole("User");
            if (isGranted)
            {
                invocation.Proceed();
            }
            else
            {
                Logger.Log("Authorization denied!");
                var result = Activator.CreateInstance(invocation.Method.ReturnType) as ResultBase;
                result.Message = "Authorization denied!";
                result.IsSucess = false;
                invocation.ReturnValue = result;
            }
        }
    }
}
