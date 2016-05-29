#region using

#endregion

#region using

using System;
using System.Linq.Expressions;

#endregion

namespace TextDetector
{
    internal class Program
    {
        private static void Main(string[] args)
        {
            Extensions.ReferencedByTyped<User, User>(user => user.Id);

            #region uiaotomation

            //do
            //{
            //    var mouse = Cursor.Position; // use Windows forms mouse code instead of WPF
            //    var element = AutomationElement.FromPoint(new Point(mouse.X, mouse.Y));
            //    if (element == null)
            //    {
            //        // no element under mouse
            //        return;
            //    }

            //    Console.WriteLine("Element at position " + mouse + " is '" + element.Current.Name + "'");

            //    object pattern;
            //    // the "Value" pattern is supported by many application (including IE & FF)
            //    if (element.TryGetCurrentPattern(ValuePattern.Pattern, out pattern))
            //    {
            //        var valuePattern = (ValuePattern) pattern;
            //        Console.WriteLine(" Value=" + valuePattern.Current.Value);
            //    }

            //    // the "Text" pattern is supported by some applications (including Notepad)and returns the current selection for example
            //    if (element.TryGetCurrentPattern(TextPattern.Pattern, out pattern))
            //    {
            //        var textPattern = (TextPattern) pattern;
            //        foreach (var range in textPattern.GetSelection())
            //        {
            //            Console.WriteLine(" SelectionRange=" + range.GetText(-1));
            //        }
            //    }
            //    Thread.Sleep(1000);
            //    Console.WriteLine();
            //    Console.WriteLine();
            //} while (true);

            #endregion
        }
    }

    internal class User
    {
        public virtual int Id { get; set; }
    }

    internal static class Extensions
    {
        public static void ReferencedByTyped<T1, T2>(Expression<Func<T2, object>> fkExpression)
        {
            var a = typeof (T1).Name;
            var memberExp = (UnaryExpression) fkExpression.Body;
            var member = (MemberExpression)memberExp.Operand;
            var memberName = member.Member.Name;
        }
    }
}