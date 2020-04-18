namespace CliApi.CommandHandling
{
    using System;
    using System.Collections.Generic;
    using System.CommandLine;

    /// <summary>
    /// Creates a set of validation logic around <see cref="Command"/>
    /// </summary>
    public class ValidatedCommand : Command
    {
        private readonly List<Func<IServiceProvider, (bool, string)>> _validations;

        public ValidatedCommand(string name, string description = null) : base(name, description)
        {
            _validations = new List<Func<IServiceProvider, (bool, string)>>();
        }

        /// <summary>
        /// Adds a validation callback before the command gets executed,
        /// also has runtime IServiceProvider to resolve required services during the validation
        /// </summary>
        /// <param name="callback"></param>
        public void AddValidation(Func<IServiceProvider, (bool IsValid, string Reason)> callback)
        {
            _validations.Add(callback);
        }

        /// <summary>
        /// Executes the command validations and returns outcome. Stops after first validation error encounter.
        /// </summary>
        /// <param name="provider"></param>
        /// <returns></returns>
        public (bool, string) Validate(IServiceProvider provider)
        {
            foreach (Func<IServiceProvider, (bool, string)> validation in _validations)
            {
                (bool isValid, string reason) = validation(provider);
                if (!isValid)
                {
                    return (false, reason);
                }
            }

            return (true, string.Empty);
        }
    }
}