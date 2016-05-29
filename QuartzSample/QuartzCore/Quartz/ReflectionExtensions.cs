using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;

using Castle.Core.Internal;

namespace QuartzCore.Quartz
{
    public static class ReflectionExtensions
    {
        public static ICollection<Assembly> FindAssemliesInBin(this Type type)
        {
            var allAssemblies = new List<Assembly>();
            var path = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);

            Directory
                .GetFiles(path, "*.dll")
                .ForEach(
                    dll => allAssemblies.Add(Assembly.LoadFile(dll))
                );

            return allAssemblies;
        }
    }
}