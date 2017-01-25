using Castle.DynamicProxy;

using DependencyInjectionCourse.Logger;

using Newtonsoft.Json;

namespace DependencyInjectionCourse.Interceptors
{
    public class AuditInterceptor : IInterceptor
    {
        private readonly ILogger _logger;

        public AuditInterceptor(ILogger logger)
        {
            _logger = logger;
        }

        public void Intercept(IInvocation invocation)
        {
            object argument = invocation.Arguments[0];
            _logger.Log($"Invoking: {invocation.Method.Name}, with parameter {invocation.Arguments[0].GetType().Name} | {JsonConvert.SerializeObject(argument)}");

            invocation.Proceed();
        }
    }
}
